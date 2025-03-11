package com.example.converter;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.s3a.S3AFileSystem;
import org.apache.parquet.avro.AvroParquetWriter;
import org.apache.parquet.hadoop.ParquetWriter;
import org.apache.parquet.hadoop.metadata.CompressionCodecName;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

public class TxtToParquetS3Converter {
    public static void main(String[] args) {
        try {
            String inputFilePath = "data.txt";  // Local input file
            String outputFileName = UUID.randomUUID() + "output.parquet";
            String outputFilePath = "s3a://attachments-service/" + outputFileName;  // S3 path

            // Load Avro schema

            var schemaPath = Paths.get(ClassLoader.getSystemClassLoader().getResource("schema.avsc").toURI());
            Schema schema = loadAvroSchema(schemaPath);
            if (schema == null) {
                System.out.println("Schema file could not be loaded.");
                return;
            }

            // Configure Hadoop for S3
            Configuration conf = configureS3();

            // Initialize Parquet writer for S3
            Path s3Path = new Path(outputFilePath);

            var inputPath = Paths.get(ClassLoader.getSystemClassLoader().getResource(inputFilePath).toURI());

            processFile(inputPath, s3Path, schema, conf);

            System.out.println("Parquet file successfully written to S3: " + outputFilePath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Configuration configureS3() {
        Configuration conf = new Configuration();
        conf.set("fs.s3a.access.key", "key");
        conf.set("fs.s3a.secret.key", "key");
        conf.set("fs.s3a.endpoint", "s3.amazonaws.com");
        conf.set("fs.s3a.impl", S3AFileSystem.class.getName());
        return conf;
    }

    private static void processFile(java.nio.file.Path inputPath, Path s3Path, Schema schema, Configuration conf) throws IOException {
        try (Stream<String> lines = Files.lines(inputPath);
             ParquetWriter<GenericRecord> writer = AvroParquetWriter.<GenericRecord>builder(s3Path)
                     .withSchema(schema)
                     .withConf(conf)
                     .withCompressionCodec(CompressionCodecName.SNAPPY)
                     .build()) {

            Iterator<String> iterator = lines.iterator();
            if (!iterator.hasNext()) {
                System.out.println("Empty input file!");
                return;
            }

            // Read header and map columns
            String headerLine = iterator.next();
            String[] headers = headerLine.split("\\|");
            Map<String, Integer> columnIndexMap = mapColumnIndexes(headers);

            // Process and write records
            iterator.forEachRemaining(line -> {
                String[] values = line.split("\\|", -1);
                GenericRecord record = createRecord(values, columnIndexMap, schema);
                try {
                    writer.write(record);
                } catch (IOException e) {
                    throw new RuntimeException("Failed to write record to Parquet file", e);
                }
            });
        }
    }

    private static GenericRecord createRecord(String[] values, Map<String, Integer> columnIndexMap, Schema schema) {
        GenericRecord record = new GenericData.Record(schema);

        record.put("transaction_id", getValue(values, columnIndexMap, ColumnName.TRANSACTION_ID));
        record.put("first_name", getValue(values, columnIndexMap, ColumnName.FIRST_NAME));
        record.put("last_name", getValue(values, columnIndexMap, ColumnName.LAST_NAME));
        record.put("middle_name", getNullableValue(values, columnIndexMap, ColumnName.MIDDLE_NAME));
        record.put("suffix", getNullableValue(values, columnIndexMap, ColumnName.SUFFIX));
        record.put("address_line1", getValue(values, columnIndexMap, ColumnName.ADDRESS_LINE1));
        record.put("address_line2", getNullableValue(values, columnIndexMap, ColumnName.ADDRESS_LINE2));
        record.put("city", getValue(values, columnIndexMap, ColumnName.CITY));
        record.put("state", getValue(values, columnIndexMap, ColumnName.STATE));
        record.put("zip_code", getValue(values, columnIndexMap, ColumnName.ZIP_CODE));
        record.put("dob", getValue(values, columnIndexMap, ColumnName.DOB));
        record.put("ssn", getValue(values, columnIndexMap, ColumnName.SSN));
        record.put("gender", getValue(values, columnIndexMap, ColumnName.GENDER));
        record.put("email", getValue(values, columnIndexMap, ColumnName.EMAIL));
        record.put("phone_number", getValue(values, columnIndexMap, ColumnName.PHONE_NUMBER));
        record.put("mm", getValue(values, columnIndexMap, ColumnName.MM));
        record.put("subscriber_id", getValue(values, columnIndexMap, ColumnName.SUBSCRIBER_ID));
        record.put("oeidp_id", getValue(values, columnIndexMap, ColumnName.OEIDP_ID));
        record.put("upi", getValue(values, columnIndexMap, ColumnName.UPI));
        record.put("return_code", "");
        record.put("match_score", "");

        return record;
    }

    private static Schema loadAvroSchema(java.nio.file.Path schemaFileName) {
        try {
            return new Schema.Parser().parse(Files.readString(schemaFileName));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static Map<String, Integer> mapColumnIndexes(String[] headers) {
        Map<String, Integer> columnIndexMap = new HashMap<>();
        for (int i = 0; i < headers.length; i++) {
            columnIndexMap.put(headers[i].trim(), i);
        }
        return columnIndexMap;
    }

    private static String getValue(String[] values, Map<String, Integer> columnIndexMap, ColumnName column) {
        return Optional.ofNullable(columnIndexMap.get(column.getKey()))
                .map(index -> values[index])
                .orElse("");
    }

    private static Object getNullableValue(String[] values, Map<String, Integer> columnIndexMap, ColumnName column) {
        return Optional.ofNullable(columnIndexMap.get(column.getKey()))
                .map(index -> values[index].trim())
                .filter(value -> !value.isEmpty())
                .orElse(null);
    }

}
