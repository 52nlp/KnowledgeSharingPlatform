package org.apache.lucene.util;

import org.apache.lucene.util.IntBlockPool.Allocator;

/*
 *
 * Copyright(c) 2015, Samsung Electronics Co., Ltd.
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.
    * Neither the name of the <organization> nor the
      names of its contributors may be used to endorse or promote products
      derived from this software without specific prior written permission.
    
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */ 

/**
 * A {@link Allocator} implementation that recycles unused int
 * blocks in a buffer and reuses them in subsequent calls to
 * {@link #getIntBlock()}.
 * <p>
 * Note: This class is not thread-safe
 * </p>
 * @lucene.internal
 */
public final class RecyclingIntBlockAllocator extends Allocator {
  private int[][] freeByteBlocks;
  private final int maxBufferedBlocks;
  private int freeBlocks = 0;
  private final Counter bytesUsed;
  public static final int DEFAULT_BUFFERED_BLOCKS = 64;

  /**
   * Creates a new {@link RecyclingIntBlockAllocator}
   * 
   * @param blockSize
   *          the block size in bytes
   * @param maxBufferedBlocks
   *          maximum number of buffered int block
   * @param bytesUsed
   *          {@link Counter} reference counting internally allocated bytes
   */
  public RecyclingIntBlockAllocator(int blockSize, int maxBufferedBlocks,
      Counter bytesUsed) {
    super(blockSize);
    freeByteBlocks = new int[maxBufferedBlocks][];
    this.maxBufferedBlocks = maxBufferedBlocks;
    this.bytesUsed = bytesUsed;
  }

  /**
   * Creates a new {@link RecyclingIntBlockAllocator}.
   * 
   * @param blockSize
   *          the size of each block returned by this allocator
   * @param maxBufferedBlocks
   *          maximum number of buffered int blocks
   */
  public RecyclingIntBlockAllocator(int blockSize, int maxBufferedBlocks) {
    this(blockSize, maxBufferedBlocks, Counter.newCounter(false));
  }

  /**
   * Creates a new {@link RecyclingIntBlockAllocator} with a block size of
   * {@link IntBlockPool#INT_BLOCK_SIZE}, upper buffered docs limit of
   * {@link #DEFAULT_BUFFERED_BLOCKS} ({@value #DEFAULT_BUFFERED_BLOCKS}).
   * 
   */
  public RecyclingIntBlockAllocator() {
    this(IntBlockPool.INT_BLOCK_SIZE, 64, Counter.newCounter(false));
  }

  @Override
  public int[] getIntBlock() {
    if (freeBlocks == 0) {
      bytesUsed.addAndGet(blockSize*RamUsageEstimator.NUM_BYTES_INT);
      return new int[blockSize];
    }
    final int[] b = freeByteBlocks[--freeBlocks];
    freeByteBlocks[freeBlocks] = null;
    return b;
  }

  @Override
  public void recycleIntBlocks(int[][] blocks, int start, int end) {
    final int numBlocks = Math.min(maxBufferedBlocks - freeBlocks, end - start);
    final int size = freeBlocks + numBlocks;
    if (size >= freeByteBlocks.length) {
      final int[][] newBlocks = new int[ArrayUtil.oversize(size,
          RamUsageEstimator.NUM_BYTES_OBJECT_REF)][];
      System.arraycopy(freeByteBlocks, 0, newBlocks, 0, freeBlocks);
      freeByteBlocks = newBlocks;
    }
    final int stop = start + numBlocks;
    for (int i = start; i < stop; i++) {
      freeByteBlocks[freeBlocks++] = blocks[i];
      blocks[i] = null;
    }
    for (int i = stop; i < end; i++) {
      blocks[i] = null;
    }
    bytesUsed.addAndGet(-(end - stop) * (blockSize * RamUsageEstimator.NUM_BYTES_INT));
    assert bytesUsed.get() >= 0;
  }

  /**
   * @return the number of currently buffered blocks
   */
  public int numBufferedBlocks() {
    return freeBlocks;
  }

  /**
   * @return the number of bytes currently allocated by this {@link Allocator}
   */
  public long bytesUsed() {
    return bytesUsed.get();
  }

  /**
   * @return the maximum number of buffered byte blocks
   */
  public int maxBufferedBlocks() {
    return maxBufferedBlocks;
  }

  /**
   * Removes the given number of int blocks from the buffer if possible.
   * 
   * @param num
   *          the number of int blocks to remove
   * @return the number of actually removed buffers
   */
  public int freeBlocks(int num) {
    assert num >= 0 : "free blocks must be >= 0 but was: "+ num;
    final int stop;
    final int count;
    if (num > freeBlocks) {
      stop = 0;
      count = freeBlocks;
    } else {
      stop = freeBlocks - num;
      count = num;
    }
    while (freeBlocks > stop) {
      freeByteBlocks[--freeBlocks] = null;
    }
    bytesUsed.addAndGet(-count*blockSize* RamUsageEstimator.NUM_BYTES_INT);
    assert bytesUsed.get() >= 0;
    return count;
  }
}