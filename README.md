# Mortar
That crap between the bricks

## I depend on Mortar & cant auto-download Mortar without Mortar...
Because plugins depend on MortarPlugin you may have some class loading issues when enabling. To work around this, simply add this anywhere in your main plugin class. 

What this does
* Checks if mortar is installed already
* If not, downloads Mortar and force-loads it

Mortar will be enabled before your plugin is enabled.

```java
static {
    try {
        URL url = new URL("https://raw.githubusercontent.com/VolmitSoftware/Mortar/master/release/Mortar.jar");
        File plugins = new File("plugins");
        Boolean foundMortar = false;
        for (File i : plugins.listFiles()) {
            if (i.isFile() && i.getName().endsWith(".jar")) {
                ZipFile file = new ZipFile(i);
                try {
                    Enumeration<? extends ZipEntry> entries = file.entries();
                    while (entries.hasMoreElements()) {
                        ZipEntry entry = entries.nextElement();
                        if ("plugin.yml".equals(entry.getName())) {
                            InputStream in = file.getInputStream(entry);
                            PluginDescriptionFile pdf = new PluginDescriptionFile(in);
                            if (pdf.getMain().equals("mortar.bukkit.plugin.MortarAPIPlugin")) {
                                foundMortar = true;
                                break;
                            }
                        }
                    }
                } catch (Throwable ex) {
                    ex.printStackTrace();
                } finally {
                    file.close();
                }
            }
        }
        if (!foundMortar) {
            System.out.println("Cannot find mortar. Attempting to download...");
            try {
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                HttpURLConnection.setFollowRedirects(false);
                con.setConnectTimeout(10000);
                con.setReadTimeout(10000);
                InputStream in = con.getInputStream();
                File mortar = new File("plugins/Mortar.jar");
                FileOutputStream fos = new FileOutputStream(mortar);
                byte[] buf = new byte[16819];
                int r = 0;
                while ((r = in.read(buf)) != -1) {
                    fos.write(buf, 0, r);
                }
                fos.close();
                in.close();
                con.disconnect();
                System.out.println("Mortar has been downloaded. Installing...");
                Bukkit.getPluginManager().loadPlugin(mortar);
            } catch (Throwable e) {
                System.out.println("Failed to download mortar! Please download it from " + url.toString());
            }
        }
    } catch (Throwable e) {
        e.printStackTrace();
    }
}
```
