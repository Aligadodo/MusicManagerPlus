package com.filemanager.backend.controller;

import com.filemanager.domain.dto.EnumOptionDTO;
import com.filemanager.plugin.impl.audioconverter.enums.AudioFormat;
import com.filemanager.plugin.impl.audioconverter.enums.Channels;
import com.filemanager.plugin.impl.audioconverter.enums.OutputDirMode;
import com.filemanager.plugin.impl.audioconverter.enums.SampleRate;
import com.filemanager.plugin.impl.advancedrename.enums.CrossDriveMode;
import com.filemanager.plugin.impl.advancedrename.enums.ProcessScope;
import com.filemanager.plugin.utils.EnumConverter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/enums")
public class EnumController {
    
    @GetMapping("/audio-formats")
    public List<EnumOptionDTO> getAudioFormats() {
        return EnumConverter.convertEnumToDTOs(AudioFormat.class);
    }
    
    @GetMapping("/output-dir-modes")
    public List<EnumOptionDTO> getOutputDirModes() {
        return EnumConverter.convertEnumToDTOs(OutputDirMode.class);
    }
    
    @GetMapping("/sample-rates")
    public List<EnumOptionDTO> getSampleRates() {
        return EnumConverter.convertEnumToDTOs(SampleRate.class);
    }
    
    @GetMapping("/channels")
    public List<EnumOptionDTO> getChannels() {
        return EnumConverter.convertEnumToDTOs(Channels.class);
    }
    
    @GetMapping("/cross-drive-modes")
    public List<EnumOptionDTO> getCrossDriveModes() {
        return EnumConverter.convertEnumToDTOs(CrossDriveMode.class);
    }
    
    @GetMapping("/process-scopes")
    public List<EnumOptionDTO> getProcessScopes() {
        return EnumConverter.convertEnumToDTOs(ProcessScope.class);
    }
    
    @GetMapping("/{enumType}")
    public List<EnumOptionDTO> getEnumOptions(@PathVariable String enumType) {
        switch (enumType.toLowerCase()) {
            case "audio-formats":
                return EnumConverter.convertEnumToDTOs(AudioFormat.class);
            case "output-dir-modes":
                return EnumConverter.convertEnumToDTOs(OutputDirMode.class);
            case "sample-rates":
                return EnumConverter.convertEnumToDTOs(SampleRate.class);
            case "channels":
                return EnumConverter.convertEnumToDTOs(Channels.class);
            case "cross-drive-modes":
                return EnumConverter.convertEnumToDTOs(CrossDriveMode.class);
            case "process-scopes":
                return EnumConverter.convertEnumToDTOs(ProcessScope.class);
            default:
                return java.util.Collections.emptyList();
        }
    }
}