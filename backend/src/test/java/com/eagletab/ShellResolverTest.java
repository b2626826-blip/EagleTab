package com.eagletab;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import com.eagletab.terminal.ShellResolver;

// 測試回傳參數是否正確
public class ShellResolverTest {

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void resolvesAnExistingWindowsShell(){
        String[] command = new ShellResolver().resolve();
        Path executable = Path.of(command[0]);
        String filename = executable.getFileName().toString().toLowerCase();

        assertThat(executable).isRegularFile();
        assertThat(filename).isIn("pwsh.exe", "powershell.exe", "cmd.exe");

        if (filename.equals("cmd.exe")) {
            assertThat(command).hasSize(1);
        } else {
            assertThat(command).containsExactly(command[0], "-NoLogo");
        }
    }
}
