/*
 * Copyright (c) 2014 DataTorrent, Inc. ALL Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.datatorrent.contrib.hds.tfile;

import java.io.IOException;
import java.util.Arrays;
import java.util.TreeMap;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.io.file.tfile.DTFile;
import org.apache.hadoop.io.file.tfile.DTFile.Reader;
import org.apache.hadoop.io.file.tfile.DTFile.Reader.Scanner;
import org.apache.hadoop.io.file.tfile.DTFile.Reader.Scanner.Entry;
import org.apache.hadoop.io.file.tfile.TFile;

import com.datatorrent.common.util.Slice;
import com.datatorrent.contrib.hds.HDSFileAccess.HDSFileReader;

/**
 * {@link DTFile} wrapper for HDSFileReader
 * <br>
 * {@link DTFile} has exact same format as {@link TFile} with a much faster {@link Reader} implementation
 * <br>
 * DTFileReader is also fully compatible with any file generated by {@link TFileWriter}. So there is no corresponding "DTFileWriter"
 * 
 *
 */
public class DTFileReader implements HDSFileReader
{
  private final Reader reader;
  private final Scanner scanner;
  private final FSDataInputStream fsdis;

  public DTFileReader(FSDataInputStream fsdis, long fileLength, Configuration conf) throws IOException
  {
    this.fsdis = fsdis;
    reader = new Reader(fsdis, fileLength, conf);
    scanner = reader.createScanner();
  }

  /**
   * Unlike the TFile.Reader.close method this will close the wrapped InputStream.
   * @see java.io.Closeable#close()
   */
  @Override
  public void close() throws IOException
  {
    scanner.close();
    reader.close();
    fsdis.close();
  }

  @Override
  public void readFully(TreeMap<Slice, byte[]> data) throws IOException
  {
    scanner.rewind();
    for (; !scanner.atEnd(); scanner.advance()) {
      Entry en = scanner.entry();
      Slice key = new Slice(en.getBlockBuffer(), en.getKeyOffset(), en.getKeyLength());
      byte[] value = Arrays.copyOfRange(en.getBlockBuffer(), en.getValueOffset(), en.getValueOffset() + en.getValueLength());
      data.put(key, value);
    }

  }

  @Override
  public void reset() throws IOException
  {
    scanner.rewind();
  }

  @Override
  public boolean seek(byte[] key) throws IOException
  {
    return scanner.seekTo(key);
  }

  @Override
  public boolean next(Slice key, Slice value) throws IOException
  {
    if (scanner.atEnd()) return false;
    Entry en = scanner.entry();

    key.buffer = en.getBlockBuffer();
    key.offset = en.getKeyOffset();
    key.length = en.getKeyLength();

    value.buffer = en.getBlockBuffer();
    value.offset = en.getValueOffset();
    value.length = en.getValueLength();

    scanner.advance();
    return true;
  }
}
