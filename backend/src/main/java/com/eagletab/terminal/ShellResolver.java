package com.eagletab.terminal;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.springframework.stereotype.Component;

// 終端機選擇方法
@Component
public class ShellResolver {
    public String[] resolve(){
        String override = System.getenv("EAGLETAB_SHELL");
        if(override != null && !override.isBlank()) {
            return new String[] {override};
        }

        if(System.getProperty("os.name").toLowerCase().contains("win")) {
            return resolveWindowShell();
        }  

        String shell = System.getenv("SHELL");
        return new String[] {
            shell == null || shell.isBlank() ? "/bin/zsh" : shell, "-l"
        };
    }
    private String[] resolveWindowShell() {
        for (String candidate : List.of("pwsh.exe", "powershell.exe", "cmd.exe")) {
            String executable = findOnPath(candidate);
            if(executable != null) {
                return candidate.equals("cmd.exe") ? new String[] {executable} : new String[] {executable, "-NoLogo"};
            }
        }
    
        throw new IllegalStateException("未找到支援的終端機");
    }

    private String findOnPath(String executable) {
        String pathValue = System.getenv("PATH");
        if (pathValue == null) {
            return null;
        }

        for (String directory : pathValue.split(File.pathSeparator)) {
            Path candicate = Path.of(directory, executable);
            if (Files.isRegularFile(candicate)) {
                return candicate.toString();
            }
        }

        return null;
    }
}



