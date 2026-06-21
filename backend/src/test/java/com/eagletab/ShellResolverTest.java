package com.eagletab;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import com.eagletab.terminal.ShellResolver;

/** 驗證 ShellResolver 在 Windows 上選出的程式與參數可以實際使用。 */
public class ShellResolverTest {

    /** 確認解析結果存在，且 PowerShell 與 cmd 使用各自正確的啟動參數。 */
    @Test
    @EnabledOnOs(OS.WINDOWS)
    void resolvesAnExistingWindowsShell(){
        String[] command = new ShellResolver().resolve();
        Path executable = Path.of(command[0]);
        String filename = executable.getFileName().toString().toLowerCase();

        assertThat(executable).isRegularFile();
        assertThat(filename).isIn("pwsh.exe", "powershell.exe", "cmd.exe");

        // cmd 不支援 -NoLogo；兩種 PowerShell 則應關閉啟動畫面。
        if (filename.equals("cmd.exe")) {
            assertThat(command).hasSize(1);
        } else {
            assertThat(command).containsExactly(command[0], "-NoLogo");
        }
    }
}
