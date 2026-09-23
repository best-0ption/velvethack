package im.best0ption.velvet.render.font;

public class FontManager {
    public static boolean inited = false;
    public static MsdfFont biko;
    public static MsdfFont sfmedium;
    public static MsdfFont icons;
    public static MsdfFont sfsemibold;
    public static MsdfFont consolas;
    public static MsdfFont sfbold;
    public static MsdfFont open;
    //CCIGANSKIY METHOD!!!!\\\
    public static void init() {
        open = new MsdfFont("open");
        biko = new MsdfFont("biko");
        icons = new MsdfFont("icons");
        sfmedium = new MsdfFont("sfmedium");
        sfbold = new MsdfFont("sfbold");
        sfsemibold = new MsdfFont("sfsemibold");
        consolas = new MsdfFont("consolas");
        inited = true;
    }
}
