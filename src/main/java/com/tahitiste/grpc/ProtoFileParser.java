package com.tahitiste.grpc;

import com.github.os72.protocjar.Protoc;
import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;
import com.tahitiste.grpc.exception.ProtoFileParseException;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ProtoFileParser {

    private ProtoFileParser() {
    }

    /**
     * <p>Parse a proto file, compile to its binary form using `protoc` compiler and return the FileDescriptor</p>
     * @param protoFilePath : the path of the proto file (*.proto)
     * @return a FileDescriptor
     * @throws ProtoFileParseException if an error occurred
     */
    public static Descriptors.FileDescriptor parse(Path protoFilePath) {

        Path tempOutputFile = createTempFile();

        generateBinaryProtoFile(protoFilePath, tempOutputFile);

        DescriptorProtos.FileDescriptorSet fileDescriptorSet = parseBinaryProtoFile(tempOutputFile);

        return getFileDescriptor(fileDescriptorSet);
    }

    private static void generateBinaryProtoFile(Path protoFile, Path tempOutputFile) {
        ByteArrayOutputStream logCompilerOutput = new ByteArrayOutputStream();
        ByteArrayOutputStream logCompilerErrorOutput = new ByteArrayOutputStream();

        int exitCode = 0;

        try {
            exitCode = Protoc.runProtoc(
                    new String[]{
                            "--proto_path=" + protoFile.getParent().toAbsolutePath().toString(),
                            "--descriptor_set_out=" + tempOutputFile.toAbsolutePath().toString(),
                            protoFile.toAbsolutePath().toString()
                    },
                    logCompilerOutput,
                    logCompilerErrorOutput);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            throw new ProtoFileParseException("Failed to read proto file", e);
        }

        if (exitCode != 0) {
            throw new ProtoFileParseException("Failed to compile proto file :  " + logCompilerOutput.toString());
        }
    }

    private static Path createTempFile() {
        Path tempOutputFile;

        try {
            tempOutputFile = Files.createTempFile(null, null);
        } catch (IOException e) {
            throw new ProtoFileParseException("Failed to create temp file used for binary Proto file creation", e);
        }
        return tempOutputFile;
    }

    private static DescriptorProtos.FileDescriptorSet parseBinaryProtoFile(Path tempOutputFile) {
        DescriptorProtos.FileDescriptorSet fileDescriptorSet;
        try {
            fileDescriptorSet = DescriptorProtos.FileDescriptorSet.parseFrom(new FileInputStream(tempOutputFile.toAbsolutePath().toFile()));
        } catch (IOException e) {
            throw new ProtoFileParseException("temp file used to create binary proto file no longer exists");
        }
        return fileDescriptorSet;
    }

    private static Descriptors.FileDescriptor getFileDescriptor(DescriptorProtos.FileDescriptorSet fileDescriptorSet) {
        try {
            return Descriptors.FileDescriptor.buildFrom(fileDescriptorSet.getFile(0), new Descriptors.FileDescriptor[]{});
        } catch (Descriptors.DescriptorValidationException e) {
            throw new ProtoFileParseException(e);
        }
    }
}
