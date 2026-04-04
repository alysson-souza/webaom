/*
 * WebAOM - Web Anime-O-Matic
 * Copyright (C) 2005-2010 epoximator 2025 Alysson Souza
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License version 2 as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, see <https://www.gnu.org/licenses/>.
 */

package epox.webaom.hash;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Throughput benchmark for {@link HashAlgorithm} implementations.
 *
 * <p>Run with: {@code ./gradlew benchmark}
 *
 * <p>This is NOT run in normal CI — it uses {@code @Tag("benchmark")}.
 */
@Tag("benchmark")
class HashBenchmarkTest {

    private static final int DATA_SIZE = 256 * 1024 * 1024; // 256 MB
    private static final int BUFFER_SIZE = 3 * 1024 * 1024; // 3 MB (matches DiskIOManager)
    private static final int WARMUP_ROUNDS = 2;
    private static final int MEASURED_ROUNDS = 5;

    @Test
    void benchmarkHashAlgorithms() {
        byte[] data = new byte[DATA_SIZE];
        new Random(42).nextBytes(data);

        System.out.println("\n=== Hash Algorithm Benchmark ===");
        System.out.println("Data size: " + (DATA_SIZE / 1024 / 1024) + " MB");
        System.out.println("Buffer size: " + (BUFFER_SIZE / 1024 / 1024) + " MB");
        System.out.println("Warmup rounds: " + WARMUP_ROUNDS);
        System.out.println("Measured rounds: " + MEASURED_ROUNDS);

        Map<String, HashAlgorithm> algorithms = new LinkedHashMap<>();
        algorithms.put("CRC32", new Crc32Hash());
        algorithms.put("ED2K", new Ed2kHash());
        algorithms.put("MD5", new Md5Hash());
        algorithms.put("SHA-1", new Sha1Hash());
        algorithms.put("TTH", new TthHash());

        System.out.printf("%n%-15s %12s %12s %12s %12s%n", "Algorithm", "Avg MB/s", "Min MB/s", "Max MB/s", "Hex");
        System.out.println("-".repeat(67));

        for (Map.Entry<String, HashAlgorithm> entry : algorithms.entrySet()) {
            BenchmarkResult result = benchmark(entry.getKey(), entry.getValue(), data);
            System.out.printf(
                    "%-15s %12.1f %12.1f %12.1f %12s%n",
                    result.name, result.avgMBps, result.minMBps, result.maxMBps, result.hexPrefix);
        }

        System.out.println();
    }

    private BenchmarkResult benchmark(String name, HashAlgorithm algorithm, byte[] data) {
        for (int i = 0; i < WARMUP_ROUNDS; i++) {
            algorithm.reset();
            hashData(algorithm, data);
        }

        double[] throughputs = new double[MEASURED_ROUNDS];
        String hex = null;

        for (int i = 0; i < MEASURED_ROUNDS; i++) {
            algorithm.reset();
            long start = System.nanoTime();
            hashData(algorithm, data);
            long elapsed = System.nanoTime() - start;
            throughputs[i] = (DATA_SIZE / 1024.0 / 1024.0) / (elapsed / 1_000_000_000.0);

            if (hex == null) {
                String fullHex = algorithm.hexValue();
                hex = fullHex.substring(0, Math.min(8, fullHex.length())) + "..";
            }
        }

        return toResult(name, throughputs, hex);
    }

    private void hashData(HashAlgorithm algorithm, byte[] data) {
        for (int offset = 0; offset < data.length; offset += BUFFER_SIZE) {
            int len = Math.min(BUFFER_SIZE, data.length - offset);
            algorithm.update(data, offset, len);
        }
    }

    private BenchmarkResult toResult(String name, double[] throughputs, String hex) {
        double sum = 0, min = Double.MAX_VALUE, max = 0;
        for (double t : throughputs) {
            sum += t;
            min = Math.min(min, t);
            max = Math.max(max, t);
        }
        return new BenchmarkResult(name, sum / MEASURED_ROUNDS, min, max, hex);
    }

    record BenchmarkResult(String name, double avgMBps, double minMBps, double maxMBps, String hexPrefix) {}
}
