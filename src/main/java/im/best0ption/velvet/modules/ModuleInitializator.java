package im.best0ption.velvet.modules;

import im.best0ption.velvet.modules.impl.player.AutoSprint;
import im.best0ption.velvet.modules.impl.render.FullBright;
import lombok.Getter;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Getter
public class ModuleInitializator {
    private final List<Module> modules = new CopyOnWriteArrayList<Module>();
    private AutoSprint autoSprint;
    private FullBright fullBright;
    public void init(){
        register(
                autoSprint = new AutoSprint(),
                fullBright = new FullBright()
        );
    }
    public void register(Module... moduless){
        Arrays.sort(moduless, Comparator.comparing(Module::getName));
        this.modules.addAll(Arrays.asList(moduless));
    }
}
