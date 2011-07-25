package org.apache.hadoop.mapreduce.lib.output;

import java.io.IOException;
import java.util.Arrays;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FileUtil;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.BloomMapFileReader;
import org.apache.hadoop.io.BloomMapFileWriter;
import org.apache.hadoop.io.MapFile;
import org.apache.hadoop.io.SequenceFile.CompressionType;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.io.WritableComparable;
import org.apache.hadoop.io.compress.CompressionCodec;
import org.apache.hadoop.mapreduce.Partitioner;
import org.apache.hadoop.mapreduce.TaskAttemptContext;

public class BloomMapFileOutputFormat<T> extends MapFileOutputFormat<T> {

    @Override
    protected MapFile.Writer createMapFileWriter(final TaskAttemptContext context, final FileSystem fs,
            final Path file, final CompressionType compressionType, final CompressionCodec codec) throws IOException {

        return new BloomMapFileWriter(context.getConfiguration(), fs, file.toString(), context.getOutputKeyClass()
                .asSubclass(WritableComparable.class), context.getOutputValueClass().asSubclass(Writable.class),
                compressionType, codec, context);
    }

    /**
     * Get an entry from output generated by this class.
     */
    public static <K extends WritableComparable<?>, V extends Writable> Writable getEntry(
            final BloomMapFileReader[] readers, final Partitioner<K, V> partitioner, final K key, final V value)
            throws IOException {

        int part = partitioner.getPartition(key, value, readers.length);
        return readers[part].get(key, value);
    }

    /**
     * Open the output generated by this format.
     */
    public static BloomMapFileReader[] getReaders(final FileSystem ignored, final Path dir, final Configuration conf)
            throws IOException {

        FileSystem fs = dir.getFileSystem(conf);
        Path[] names = FileUtil.stat2Paths(fs.listStatus(dir));

        // sort names, so that hash partitioning works
        Arrays.sort(names);

        BloomMapFileReader[] parts = new BloomMapFileReader[names.length];
        for (int i = 0; i < names.length; i++) {
            parts[i] = new BloomMapFileReader(fs, names[i].toString(), conf);
        }

        return parts;
    }
}