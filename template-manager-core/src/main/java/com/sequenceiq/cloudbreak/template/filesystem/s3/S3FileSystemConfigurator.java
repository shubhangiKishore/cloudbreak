package com.sequenceiq.cloudbreak.template.filesystem.s3;

import static com.sequenceiq.common.model.FileSystemType.S3;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.template.filesystem.AbstractFileSystemConfigurator;
import com.sequenceiq.common.model.FileSystemType;

@Component
public class S3FileSystemConfigurator extends AbstractFileSystemConfigurator<S3FileSystemConfigurationsView> {

    private static final Logger LOGGER = LoggerFactory.getLogger(S3FileSystemConfigurator.class);

    @Override
    public FileSystemType getFileSystemType() {
        return S3;
    }

    @Override
    public String getProtocol() {
        return S3.getProtocol();
    }
}
