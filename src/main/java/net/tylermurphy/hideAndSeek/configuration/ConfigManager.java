/*
 * This file is part of Kenshins Hide and Seek
 *
 * Copyright (c) 2021 Tyler Murphy.
 *
 * Kenshins Hide and Seek free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * he Free Software Foundation version 3.
 *
 * Kenshins Hide and Seek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 */

package net.tylermurphy.hideAndSeek.configuration;

import net.tylermurphy.hideAndSeek.Main;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class ConfigManager {

    private final File file;
    private YamlConfiguration config,defaultConfig;
    private String defaultFilename;

    public ConfigManager(String filename){
        this.file = new File(Main.data, filename);
        this.defaultFilename = file.getName();

        File folder = Main.data;
        if(!folder.exists()){
            if(!folder.mkdirs()){
                throw new RuntimeException("Failed to make directory: " + file.getPath());
            }
        }

        if(!file.exists()){
            saveDefaultConfiguration();
        }

        this.config = YamlConfiguration.loadConfiguration(file);

        InputStream input = Main.plugin.getResource(file.getName());
        if(input == null){
            throw new RuntimeException("Could not create input stream for "+file.getPath());
        }
        InputStreamReader reader = new InputStreamReader(input);
        this.defaultConfig = YamlConfiguration.loadConfiguration(reader);
        try{
            input.close();
            reader.close();
        } catch (IOException ignored){}
    }

    public ConfigManager(String filename, String defaultFilename){

        this.defaultFilename = defaultFilename;
        this.file = new File(Main.data, filename);

        if(!file.exists()){
            saveDefaultConfiguration();
        }

        this.config = YamlConfiguration.loadConfiguration(file);

        InputStream input = Main.plugin.getResource(defaultFilename);
        if(input == null){
            throw new RuntimeException("Could not create input stream for "+defaultFilename);
        }
        InputStreamReader reader = new InputStreamReader(input);
        this.defaultConfig = YamlConfiguration.loadConfiguration(reader);
        try{
            input.close();
            reader.close();
        } catch (IOException e){
            Main.plugin.getLogger().severe("Couldn't find "+defaultFilename+" internally. Did you set an incorrect local?");
            Main.plugin.getServer().getPluginManager().disablePlugin(Main.plugin);
            throw new RuntimeException();
        }
    }

    private void saveDefaultConfiguration(){
        try{
            InputStream input = Main.plugin.getResource(defaultFilename);
            if(input == null){
                throw new RuntimeException("Could not create input stream for "+defaultFilename);
            }
            java.nio.file.Files.copy(input, file.toPath());
            input.close();
        } catch(IOException e){
            e.printStackTrace();
        }
    }

    public double getDouble(String path){
        double value = config.getDouble(path);
        if(value == 0.0D){
            return defaultConfig.getDouble(path);
        } else {
            return value;
        }
    }

    public int getInt(String path){
        int value = config.getInt(path);
        if(value == 0){
            return defaultConfig.getInt(path);
        } else {
            return value;
        }
    }

    public String getString(String path){
        String value = config.getString(path);
        if(value == null){
            return defaultConfig.getString(path);
        } else {
            return value;
        }
    }

    public void reset(String path){
        config.set(path, defaultConfig.get(path));
    }

    public void resetFile(String newDefaultFilename){
        this.defaultFilename = newDefaultFilename;

        InputStream input = Main.plugin.getResource(defaultFilename);
        if(input == null){
            throw new RuntimeException("Could not create input stream for "+defaultFilename);
        }
        InputStreamReader reader = new InputStreamReader(input);
        this.config = YamlConfiguration.loadConfiguration(reader);
        this.defaultConfig = YamlConfiguration.loadConfiguration(reader);

    }

    public boolean getBoolean(String path){
        boolean value = config.getBoolean(path);
        if(!value){
            return defaultConfig.getBoolean(path);
        } else {
            return true;
        }
    }

    public ConfigurationSection getConfigurationSection(String path){
        ConfigurationSection section = config.getConfigurationSection(path);
        if(section == null){
            return defaultConfig.getConfigurationSection(path);
        } else {
            return section;
        }
    }

    public void set(String path, Object value){
        config.set(path, value);
    }

    public void saveConfig(){
        try {
            InputStream is = Main.plugin.getResource(defaultFilename);
            if(is == null){
                throw new RuntimeException("Could not create input stream for "+defaultFilename);
            }
            StringBuilder textBuilder = new StringBuilder();
            Reader reader = new BufferedReader(new InputStreamReader(is, Charset.forName(StandardCharsets.UTF_8.name())));
            int c;
            while((c = reader.read()) != -1){
                textBuilder.append((char) c);
            }
            String yamlString = textBuilder.toString();
            Map<String, Object> temp = config.getValues(true);
            for(Map.Entry<String, Object> entry: temp.entrySet()){
                if(entry.getValue() instanceof Integer || entry.getValue() instanceof Double || entry.getValue() instanceof String || entry.getValue() instanceof Boolean){
                    String[] parts = entry.getKey().split("\\.");
                    int index = 0;
                    int i = 0;
                    for(String part : parts) {
                        if(i == 0) {
                            index = yamlString.indexOf(part+":", index);
                        } else {
                            index = yamlString.indexOf(" " + part+":", index);
                            index++;
                        }
                        i++;
                        if(index == -1) break;
                    }
                    if(index < 10)  continue;
                    int start = yamlString.indexOf(' ', index);
                    int end = yamlString.indexOf('\n', index);
                    if(end == -1) end = yamlString.length();
                    String replace = entry.getValue().toString();
                    if(entry.getValue() instanceof String){
                        replace = "\"" + replace + "\"";
                    }
                    StringBuilder builder = new StringBuilder(yamlString);
                    builder.replace(start+1, end, replace);
                    yamlString = builder.toString();
                }
            }
            PrintWriter out = new PrintWriter(file);
            out.print(yamlString);
            out.close();
        } catch (IOException e){
            e.printStackTrace();
        }
    }

}
