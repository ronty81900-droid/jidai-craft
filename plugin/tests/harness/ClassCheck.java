import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import org.bukkit.plugin.java.JavaPlugin;

/** Loads the built plugin class and checks it has the shape Bukkit requires. */
public class ClassCheck {
    public static void main(String[] args) throws Exception {
        File jar = new File(args[0]);
        String mainClass = args[1];
        int ok = 0, total = 0;

        try (URLClassLoader cl = new URLClassLoader(new URL[]{jar.toURI().toURL()},
                ClassCheck.class.getClassLoader())) {
            Class<?> c = Class.forName(mainClass, false, cl);
            total++; System.out.println("[PASS] class loads: " + c.getName()); ok++;

            total++;
            boolean isPlugin = JavaPlugin.class.isAssignableFrom(c);
            System.out.println((isPlugin ? "[PASS]" : "[FAIL]") + " extends JavaPlugin = " + isPlugin);
            if (isPlugin) ok++;

            total++;
            boolean noArgCtor = false;
            try { c.getDeclaredConstructor(); noArgCtor = true; } catch (NoSuchMethodException ignored) {}
            System.out.println((noArgCtor ? "[PASS]" : "[FAIL]") + " has no-arg constructor = " + noArgCtor);
            if (noArgCtor) ok++;

            for (String name : new String[]{"onEnable", "onDisable"}) {
                total++;
                Method m = null;
                try { m = c.getDeclaredMethod(name); } catch (NoSuchMethodException ignored) {}
                boolean good = m != null && m.getReturnType() == void.class
                        && java.lang.reflect.Modifier.isPublic(m.getModifiers());
                System.out.println((good ? "[PASS]" : "[FAIL]") + " public void " + name + "() = " + good);
                if (good) ok++;
            }

            total++;
            int major = classFileMajor(cl, mainClass);
            // ★1.20.1 のサーバーは Java 17 で動く。61 = Java 17。
            //   65(Java 21) のままだと 1.20.1 の Paper が読み込めない。
            boolean j17 = major == 61;
            System.out.println((j17 ? "[PASS]" : "[FAIL]") + " class file version = " + major
                    + " (61 = Java 17 / 1.20.1 のサーバーはこれ)");
            if (j17) ok++;
        }
        System.out.println("RESULT: " + ok + "/" + total);
    }

    private static int classFileMajor(ClassLoader cl, String name) throws Exception {
        try (var in = cl.getResourceAsStream(name.replace('.', '/') + ".class")) {
            byte[] head = in.readNBytes(8);
            return ((head[6] & 0xFF) << 8) | (head[7] & 0xFF);
        }
    }
}
