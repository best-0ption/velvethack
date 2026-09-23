#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
improve_gifs.py — пакетное улучшение GIF-анимаций мода Velvet.

Пайплайн (на каждый GIF, строго последовательно; GPU-инструменты по одному,
параллельный запуск Real-ESRGAN вызывает Xid 109 и чёрные кадры):

  1. ffmpeg: извлечение кадров (-fps_mode passthrough) -> rgba, альфа-маска
  1b. defringe: цветное расширение (BFS-заливка цветом ближайшего непрозрачного
      пикселя) — каждый прозрачный тексел получает «свой» цвет элемента, чтобы
      ESRGAN/RIFE/даунскейл не тянули белый фон в контур (белая обводка при
      GPU-сэмплинге с LINEAR-фильтрацией и мипмапами)
  2. Real-ESRGAN animevideov3 x2 (-j 1:1:1, -t 128) только по RGB
     (альфа не апскейлится, под прозрачностью чистый белый -> нет чёрных ореолов)
  3. ffmpeg: даунскейл lanczos обратно к исходному размеру
  4. RIFE v4.6: интерполяция RGB и альфы до целевого FPS
  5. ffmpeg: format=gray + lutyuv (бинаризация альфы) + alphamerge
  6. ffmpeg: palettegen=stats_mode=diff + paletteuse=dither=sierra2_4a, -loop 0
  7. gifsicle -O3 (lossless)
  8. проверки: чёрные кадры (YMAX<8 по промежуточным кадрам), длительность,
     декодирование их реальным GifDecoder'ом (ValidateGif, если доступен JDK)

Результат: <animations>/_improved/<имя>.gif  (оригиналы не изменяются)

Использование:
  ./tools/improve_gifs.py                 # все 8 гифок
  ./tools/improve_gifs.py notfound like   # только выбранные
  ./tools/improve_gifs.py --fps 100       # другой целевой FPS
"""

from __future__ import annotations

import argparse
import json
import re
import shutil
import subprocess
import sys
import tempfile
import time
from collections import deque
from pathlib import Path

REPO = Path(__file__).resolve().parents[1]
ANIM_DIR = REPO / "src/main/resources/assets/velvet/textures/animations"
OUT_SUBDIR = "improved"

TOOLS = Path.home() / ".cache/velvet-gif-tools"
ESRGAN_BIN = TOOLS / "realesrgan-ncnn-vulkan"
RIFE_BIN = TOOLS / "rife-ncnn-vulkan"
ESR_MODELS = TOOLS / "models"
RIFE_MODEL = TOOLS / "rife-v4.6"
ESR_MODEL_NAME = "realesr-animevideov3"

JAVA_BIN = Path("/tmp/opencode/jdk/jdk-25.0.4.1+1/bin/java")
VALIDATE_DIR = Path("/tmp/opencode/validate")
CLASSES = REPO / "build/classes/java/main"

DEFAULT_FPS = 60
ALPHA_THRESHOLD = 127   # lutyuv: >127 -> 255 (ровно на середине, проверено)
BLACK_YMAX = 8          # YMAX < 8 => кадр чёрный (у нас фон всегда белый)

YMAX_RE = re.compile(r"lavfi\.signalstats\.YMAX=(\d+)")
FRAME_RE = re.compile(r"^frame:(\d+)")


def log(msg: str = "") -> None:
    print(msg, flush=True)


def run(cmd, desc: str = ""):
    """Запуск команды; при ошибке — stderr и исключение."""
    cmd = [str(c) for c in cmd]
    t0 = time.time()
    p = subprocess.run(cmd, capture_output=True, text=True)
    dt = time.time() - t0
    if p.returncode != 0:
        sys.stderr.write((p.stderr or p.stdout or "")[-8000:])
        raise RuntimeError(f"команда провалилась (rc={p.returncode}): {desc or cmd[0]}")
    return dt


def probe(gif: Path) -> dict:
    """Характеристики GIF через ffprobe."""
    p = subprocess.run(
        ["ffprobe", "-v", "error", "-select_streams", "v:0",
         "-show_entries", "stream=width,height,nb_frames:format=duration",
         "-of", "json", str(gif)],
        capture_output=True, text=True, check=True)
    j = json.loads(p.stdout)
    st = j["streams"][0]
    return {
        "w": st["width"], "h": st["height"],
        "n": int(st["nb_frames"]), "dur": float(j["format"]["duration"]),
        "size": gif.stat().st_size,
    }


def png_range(d: Path) -> tuple[int, int]:
    """(первый номер, кол-во) среди %08d.png; проверка непрерывности."""
    ns = sorted(int(p.stem) for p in d.glob("*.png"))
    if not ns:
        raise RuntimeError(f"нет кадров в {d}")
    if ns[-1] - ns[0] + 1 != len(ns):
        raise RuntimeError(f"дыра в нумерации {d}: {ns[:3]} ... {ns[-3:]}")
    return ns[0], len(ns)


def defringe_frames(src_dir: Path, dst_dir: Path, w: int, h: int,
                     n_frames: int) -> float:
    """
    Цветное расширение: каждый прозрачный пиксель получает цвет БЛИЖАЙШЕГО
    непрозрачного (multi-source BFS по 4-связности). В отличие от многопроходной
    эрозии нет «мин-дрейфа» (хвост не темнеет до глобального минимума) и нет
    ограничения радиуса — ESRGAN/RIFE/даунскейл рядом с контуром видят только
    настоящие цвета элемента, поэтому белёсая обводка не появляется.
    Внутри: png-директория -> raw rgba (ffmpeg) -> BFS -> raw rgb24 (ffmpeg).
    """
    t0 = time.time()
    fs = w * h * 4
    # топология соседей одинакова для всех кадров — считаем один раз
    neigh = [[] for _ in range(w * h)]
    for y in range(h):
        for x in range(w):
            i = y * w + x
            if x > 0:
                neigh[i].append(i - 1)
            if x < w - 1:
                neigh[i].append(i + 1)
            if y > 0:
                neigh[i].append(i - w)
            if y < h - 1:
                neigh[i].append(i + w)

    pin = subprocess.Popen(
        ["ffmpeg", "-v", "error", "-start_number", "0",
         "-i", str(src_dir / "%08d.png"),
         "-f", "rawvideo", "-pix_fmt", "rgba", "-"],
        stdout=subprocess.PIPE)
    pout = subprocess.Popen(
        ["ffmpeg", "-v", "error", "-f", "rawvideo", "-pix_fmt", "rgb24",
         "-s", f"{w}x{h}", "-framerate", "25", "-i", "-",
         "-fps_mode", "passthrough", "-start_number", "0",
         str(dst_dir / "%08d.png")],
        stdin=subprocess.PIPE)
    try:
        for _ in range(n_frames):
            buf = pin.stdout.read(fs)
            if len(buf) < fs:
                raise RuntimeError("defringe: ffmpeg не отдал все кадры")
            color = bytearray(w * h * 3)
            seen = bytearray(w * h)
            dq = deque()
            for i in range(w * h):          # источники — непрозрачные пиксели
                if buf[i * 4 + 3] > 127:
                    seen[i] = 1
                    s, c = i * 4, i * 3
                    color[c] = buf[s]
                    color[c + 1] = buf[s + 1]
                    color[c + 2] = buf[s + 2]
                    dq.append(i)
            while dq:
                i = dq.popleft()
                c = i * 3
                for j in neigh[i]:
                    if not seen[j]:
                        seen[j] = 1
                        jc = j * 3
                        color[jc] = color[c]
                        color[jc + 1] = color[c + 1]
                        color[jc + 2] = color[c + 2]
                        dq.append(j)
            pout.stdin.write(bytes(color))
    finally:
        pout.stdin.close()
        pin.stdout.close()
    if pin.wait() != 0 or pout.wait() != 0:
        raise RuntimeError("defringe: ffmpeg завершился с ошибкой")
    return time.time() - t0


def black_frames(d: Path, meta_file: Path, label: str) -> list[int]:
    """
    Индексы полностью чёрных кадров (YMAX < BLACK_YMAX).
    Срабатывает именно на поломку GPU-инструмента: у наших промежуточных
    RGB-кадров фон всегда белый, а у альфа-масок — белые фигуры.
    """
    first, _ = png_range(d)
    meta_file.unlink(missing_ok=True)
    run(["ffmpeg", "-v", "error", "-start_number", str(first),
         "-i", str(d / "%08d.png"),
         "-vf", f"signalstats,metadata=print:file={meta_file}",
         "-f", "null", "-"], f"signalstats({label})")
    bad, frame = [], -1
    for line in meta_file.read_text().splitlines():
        m = FRAME_RE.match(line)
        if m:
            frame = int(m.group(1))
        m = YMAX_RE.search(line)
        if m and int(m.group(1)) < BLACK_YMAX:
            bad.append(frame)
    if bad:
        raise RuntimeError(f"чёрные кадры в {label}: {bad[:10]} (из {label})")
    return bad


def validate_gif(gif: Path) -> str | None:
    """Раскодировать их реальным GifDecoder.class; None, если харнесс недоступен."""
    if not (JAVA_BIN.exists() and VALIDATE_DIR.is_dir() and CLASSES.is_dir()):
        return None
    p = subprocess.run(
        [str(JAVA_BIN), "-cp", f"{CLASSES}:{VALIDATE_DIR}", "ValidateGif", str(gif)],
        capture_output=True, text=True)
    out = (p.stdout + p.stderr).strip().splitlines()
    text = out[-1] if out else ""
    if p.returncode != 0 or "OK" not in text:
        raise RuntimeError(f"валидация декодером провалилась: {text}")
    return text


def improve(src: Path, out_dir: Path, fps: int, keep_temp: bool) -> dict:
    info = probe(src)
    n0, dur0 = info["n"], info["dur"]
    target = max(n0 + 1, round(dur0 * fps))  # кол-во кадров при целевом FPS
    name = src.name
    log(f"[{name}] исходник: {n0} кадров, {n0/dur0:.1f} fps, "
        f"{info['w']}x{info['h']}, {info['size']//1024} КБ -> цель {target} кадров @{fps}fps")

    tmp_root = Path(tempfile.gettempdir())
    if Path("/tmp/opencode").is_dir():
        tmp_root = Path("/tmp/opencode")
    tmp = Path(tempfile.mkdtemp(prefix="velvetgif_", dir=tmp_root))
    ok = False
    try:
        for sub in ("src", "rgb_ext", "alpha", "esr", "up", "frgb", "falpha"):
            (tmp / sub).mkdir()
        meta = tmp / "stats.txt"
        w, h = info["w"], info["h"]

        # 1. Извлечение кадров -------------------------------------------------
        t = run(["ffmpeg", "-y", "-v", "error",
                 "-i", str(src), "-fps_mode", "passthrough",
                 "-start_number", "0", "-pix_fmt", "rgba",
                 str(tmp / "src" / "%08d.png")], "extract rgba")
        first, cnt = png_range(tmp / "src")
        if first != 0 or cnt != n0:
            raise RuntimeError(f"извлечено {cnt} (с {first}) кадров, ожидалось {n0}")
        log(f"  extract            {t:5.1f}с  ({cnt} кадров rgba)")

        run(["ffmpeg", "-y", "-v", "error", "-start_number", "0",
             "-i", str(tmp / "src" / "%08d.png"), "-fps_mode", "passthrough",
             "-vf", "alphaextract", "-pix_fmt", "rgb24", "-start_number", "0",
             str(tmp / "alpha" / "%08d.png")], "alpha -> rgb24-gray")

        # 1b. Defringe: каждый прозрачный тексел получает цвет ближайшего
        #     непрозрачного (BFS). Никакого белого рядом с контуром — весь
        #     дальнейший пайплайн его не утянет в видимую кромку.
        t = defringe_frames(tmp / "src", tmp / "rgb_ext", w, h, n0)
        if png_range(tmp / "rgb_ext")[1] != n0:
            raise RuntimeError("defringe: количество кадров изменилось")
        log(f"  defringe           {t:5.1f}с  (цветное расширение, ближайший цвет)")

        # 2. Real-ESRGAN x2 по RGB (строго серийно!) ---------------------------
        t = run([ESRGAN_BIN, "-i", tmp / "rgb_ext", "-o", tmp / "esr",
                 "-n", ESR_MODEL_NAME, "-s", "2", "-f", "png",
                 "-m", ESR_MODELS, "-t", "128", "-j", "1:1:1"], "esrgan")
        if png_range(tmp / "esr")[1] != n0:
            raise RuntimeError("ESRGAN: количество кадров изменилось")
        black_frames(tmp / "esr", meta, "esrgan")
        log(f"  esrgan x2 (serial) {t:5.1f}с  (RGB-only, чёрных кадров нет)")

        # 3. Даунскейл lanczos обратно ----------------------------------------
        run(["ffmpeg", "-y", "-v", "error", "-start_number", "0",
             "-i", str(tmp / "esr" / "%08d.png"), "-fps_mode", "passthrough",
             "-vf", f"scale={w}:{h}:flags=lanczos", "-pix_fmt", "rgb24",
             "-start_number", "0", str(tmp / "up" / "%08d.png")], "lanczos downscale")

        # 4. RIFE v4.6: RGB + альфа -------------------------------------------
        if target > n0:
            t1 = run([RIFE_BIN, "-i", tmp / "up", "-o", tmp / "frgb",
                      "-n", target, "-m", RIFE_MODEL], "rife rgb")
            t2 = run([RIFE_BIN, "-i", tmp / "alpha", "-o", tmp / "falpha",
                      "-n", target, "-m", RIFE_MODEL], "rife alpha")
            n_rgb, n_alf = png_range(tmp / "frgb")[1], png_range(tmp / "falpha")[1]
            if n_rgb != n_alf:
                raise RuntimeError(f"RIFE: рассинхрон кадров rgb={n_rgb} alpha={n_alf}")
            if abs(n_rgb - target) > 2:
                raise RuntimeError(f"RIFE: ожидали {target} кадров, получили {n_rgb}")
            black_frames(tmp / "frgb", meta, "rife-rgb")
            black_frames(tmp / "falpha", meta, "rife-alpha")
            log(f"  rife v4.6          {t1 + t2:5.1f}с  (rgb+alpha -> {n_rgb} кадров, "
                f"чёрных нет)")
        else:
            for sub in ("frgb", "falpha"):
                for p in (tmp / ("up" if sub == "frgb" else "alpha")).glob("*.png"):
                    shutil.copy2(p, tmp / sub / p.name)
            n_rgb = n0
            log(f"  rife               пропущено (исходник уже >= {fps}fps)")

        # 5+6. Альфа -> бинарная, alphamerge, палитра, loop=0 ------------------
        f_rgb, _ = png_range(tmp / "frgb")
        f_alf, _ = png_range(tmp / "falpha")
        step_gif = tmp / "step.gif"
        filter_complex = (
            f"[1:v]format=gray,lutyuv=y='if(gt(val,{ALPHA_THRESHOLD}),255,0)'[al];"
            "[0:v]format=rgba[bg];"
            "[bg][al]alphamerge,split[a][b];"
            "[a]palettegen=stats_mode=diff[p];"
            "[b][p]paletteuse=dither=sierra2_4a"
        )
        t = run(["ffmpeg", "-y", "-v", "error",
                 "-framerate", str(fps), "-start_number", str(f_rgb),
                 "-i", str(tmp / "frgb" / "%08d.png"),
                 "-framerate", str(fps), "-start_number", str(f_alf),
                 "-i", str(tmp / "falpha" / "%08d.png"),
                 "-filter_complex", filter_complex,
                 "-loop", "0", str(step_gif)], "palette + alphamerge")
        log(f"  palette/dither     {t:5.1f}с  (sierra2_4a, loop=0)")

        # 7. gifsicle -O3 ------------------------------------------------------
        out_gif = out_dir / name
        out_gif.unlink(missing_ok=True)
        t = run(["gifsicle", "-O3", f"--output={out_gif}", step_gif], "gifsicle -O3")
        log(f"  gifsicle -O3       {t:5.1f}с")

        # 8. Проверки ----------------------------------------------------------
        out = probe(out_gif)
        if abs(out["dur"] - dur0) > max(0.06, dur0 * 0.03):
            raise RuntimeError(f"длительность изменилась: {dur0:.3f}s -> {out['dur']}s")
        vtxt = validate_gif(out_gif)

        log(f"  результат: {out['n']} кадров, {out['n']/out['dur']:.1f} fps, "
            f"{out['size']//1024} КБ, {out['dur']:.3f}s")
        if vtxt:
            log(f"  GifDecoder: {vtxt}")

        rec = {
            "name": name, "n0": n0, "fps0": n0 / dur0, "sz0": info["size"],
            "n1": out["n"], "fps1": out["n"] / out["dur"], "sz1": out["size"],
            "dur0": dur0, "dur1": out["dur"], "validate": vtxt or "—",
            "status": "OK",
        }
        ok = True
        return rec
    except Exception:
        log(f"  !! ОШИБКА; временные файлы сохранены: {tmp}")
        raise
    finally:
        if ok and not keep_temp:
            shutil.rmtree(tmp, ignore_errors=True)


def print_report(records: list[dict]) -> None:
    log()
    log("=" * 110)
    log(f"{'файл':<14} {'было':<26} {'стало':<26} {'размер':<16} {'декодер мода'}")
    log("-" * 110)
    for r in records:
        if r["status"] != "OK":
            log(f"{r['name']:<14} ОШИБКА: {r['error']}")
            continue
        was = f"{r['n0']} кадр. {r['fps0']:.1f} fps"
        now = f"{r['n1']} кадр. {r['fps1']:.1f} fps"
        sz = f"{r['sz0']//1024} -> {r['sz1']//1024} КБ"
        log(f"{r['name']:<14} {was:<26} {now:<26} {sz:<16} {r['validate']}")
    log("=" * 110)


def main() -> int:
    ap = argparse.ArgumentParser(description="Улучшение GIF Velvet (ESRGAN + RIFE + dither)")
    ap.add_argument("gifs", nargs="*",
                    help="имена или пути (по умолчанию — все в animations/)")
    ap.add_argument("--fps", type=int, default=DEFAULT_FPS,
                    help=f"целевой FPS (по умолчанию {DEFAULT_FPS})")
    ap.add_argument("--out", default=None,
                    help="выходная папка (по умолчанию <animations>/{})".format(OUT_SUBDIR))
    ap.add_argument("--keep-temp", action="store_true",
                    help="не удалять временные папки")
    args = ap.parse_args()

    # проверка инструментов
    missing = [str(p) for p in (ESRGAN_BIN, RIFE_BIN) if not p.exists()]
    missing += [d for d in (ESR_MODELS, RIFE_MODEL) if not d.is_dir()]
    for exe in ("ffmpeg", "ffprobe", "gifsicle"):
        if not shutil.which(exe):
            missing.append(exe)
    if missing:
        log("нет инструментов: " + ", ".join(missing))
        return 2

    # список исходников
    if args.gifs:
        srcs = []
        for g in args.gifs:
            p = Path(g)
            if not p.exists() and not p.suffix:
                p = p.with_suffix(".gif")
            if not p.exists():
                p = ANIM_DIR / p.name
            if not p.exists():
                log(f"не найден: {g}")
                return 2
            srcs.append(p)
    else:
        srcs = sorted(ANIM_DIR.glob("*.gif"))
    if not srcs:
        log(f"гифки не найдены в {ANIM_DIR}")
        return 2

    out_dir = Path(args.out) if args.out else ANIM_DIR / OUT_SUBDIR
    out_dir.mkdir(parents=True, exist_ok=True)

    log(f"Источник: {ANIM_DIR}\nВыход:    {out_dir}\n"
        f"Целевой FPS: {args.fps} | гифок: {len(srcs)}\n"
        f"(! не запускайте копии скрипта параллельно — GPU-инструменты только серийно)\n")

    records = []
    t_all = time.time()
    for src in srcs:
        try:
            records.append(improve(src, out_dir, args.fps, args.keep_temp))
        except Exception as e:
            log(f"[{src.name}] ОШИБКА: {e}")
            records.append({"name": src.name, "status": "ERR", "error": str(e)})
    log(f"\nВсего: {time.time() - t_all:.0f}с")
    print_report(records)
    return 0 if all(r["status"] == "OK" for r in records) else 1


if __name__ == "__main__":
    sys.exit(main())
