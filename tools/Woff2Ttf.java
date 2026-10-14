import java.io.*; import java.nio.*; import java.nio.file.*; import java.util.zip.Inflater;
/** Unwraps a WOFF 1.0 file into a plain TTF/OTF: java Woff2Ttf.java in.woff out.ttf */
public class Woff2Ttf { public static void main(String[] a) throws Exception {
  ByteBuffer in = ByteBuffer.wrap(Files.readAllBytes(Path.of(a[0])));
  if (in.getInt(0) != 0x774F4646) throw new IOException("not WOFF");
  int flavor = in.getInt(4), n = in.getShort(12) & 0xFFFF;
  int[] tag = new int[n], off = new int[n], comp = new int[n], orig = new int[n], sum = new int[n];
  for (int i = 0; i < n; i++) { int p = 44 + i * 20; tag[i]=in.getInt(p); off[i]=in.getInt(p+4); comp[i]=in.getInt(p+8); orig[i]=in.getInt(p+12); sum[i]=in.getInt(p+16); }
  int es = Integer.highestOneBit(n), sel = Integer.numberOfTrailingZeros(es);
  int hdr = 12 + 16 * n, total = hdr; for (int i = 0; i < n; i++) total += (orig[i] + 3) & ~3;
  ByteBuffer o = ByteBuffer.allocate(total);
  o.putInt(flavor).putShort((short) n).putShort((short) (es * 16)).putShort((short) sel).putShort((short) (n * 16 - es * 16));
  int pos = hdr;
  for (int i = 0; i < n; i++) {
    byte[] data = new byte[orig[i]];
    if (comp[i] < orig[i]) { Inflater inf = new Inflater(); inf.setInput(in.array(), off[i], comp[i]); int got = inf.inflate(data); if (got != orig[i]) throw new IOException("short table"); inf.end(); }
    else System.arraycopy(in.array(), off[i], data, 0, orig[i]);
    o.putInt(12 + 16 * i, tag[i]); o.putInt(16 + 16 * i, sum[i]); o.putInt(20 + 16 * i, pos); o.putInt(24 + 16 * i, orig[i]);
    o.put(pos, data); pos += (orig[i] + 3) & ~3;
  }
  Files.write(Path.of(a[1]), o.array());
}}
