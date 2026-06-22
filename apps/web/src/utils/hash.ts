import SparkMD5 from 'spark-md5';

const CHUNK_SIZE = 5 * 1024 * 1024;

export function computeMD5(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const spark = new SparkMD5.ArrayBuffer();
    const reader = new FileReader();
    let currentChunk = 0;
    const totalChunks = Math.ceil(file.size / CHUNK_SIZE);

    function loadNext() {
      const start = currentChunk * CHUNK_SIZE;
      const end = Math.min(start + CHUNK_SIZE, file.size);
      reader.readAsArrayBuffer(file.slice(start, end));
    }

    reader.onload = (e) => {
      spark.append(e.target!.result as ArrayBuffer);
      currentChunk++;
      if (currentChunk < totalChunks) {
        loadNext();
      } else {
        resolve(spark.end());
      }
    };

    reader.onerror = () => reject(new Error('计算文件哈希失败'));

    loadNext();
  });
}
