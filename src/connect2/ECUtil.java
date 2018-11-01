package connect2;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ECUtil {
	static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();

    public static String hexEncode(byte[] buf)
    {
        char[] chars = new char[2 * buf.length];
        for (int i = 0; i < buf.length; ++i)
        {
            chars[2 * i] = HEX_CHARS[(buf[i] & 0xF0) >>> 4];
            chars[2 * i + 1] = HEX_CHARS[buf[i] & 0x0F];
        }
        return new String(chars);
    }

    public static byte[] hexDecode(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len - 1; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                                 + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    public static byte[] shortToByteArray(short s) {
		return new byte[] { (byte) ((s & 0xFF00) >> 8), (byte) (s & 0x00FF) };
	}

	public static int unsignedShort(short s) {
		byte[] array = shortToByteArray(s);
		int b1 = (0x000000FF & ((int) array[0]));
		int b2 = (0x000000FF & ((int) array[1]));
		return (b1 << 8 | b2);
	}

	public static int unsignedByte(byte b) {
		return (0x000000FF & ((int)b));
	}

	public static String getTerminatedString(ByteBuffer buf) {
		return new String(getTerminatedArray(buf));
	}

	public static byte[] getTerminatedArray(ByteBuffer buf) {
		int start = buf.position();

		while(buf.get() != 0) {}
		int end = buf.position();

		byte[] bytes = new byte[end - start - 1]; //don't include terminator
		buf.position(start);
		buf.get(bytes);

		//put position after array
		buf.position(end); //skip terminator

		return bytes;
	}

	public static byte[] strToBytes(String str) {
		return str.getBytes(StandardCharsets.UTF_8);
	}

	public static byte[] encodeStatString(byte[] data)
	{
		byte Mask = 1;
		List<Byte> result = new ArrayList<Byte>();

		for(int i = 0; i < data.length; i++) {
			if(data[i] % 2 == 0)
				result.add((byte) (data[i] + 1));
			else {
				result.add( data[i] );
				Mask |= 1 << ((i % 7) + 1);
			}

			if(i % 7 == 6 || i == data.length - 1) {
				result.add(result.size() - 1 - (i % 7), Mask);
				Mask = 1;
			}
		}

		byte[] array = new byte[result.size()];

		for(int i = 0; i < result.size(); i++) {
			array[i] = result.get(i);
		}

		return array;
	}

	public static byte[] decodeStatString(byte[] data) {
		byte Mask = 0;
		ByteBuffer result = ByteBuffer.allocate(data.length);

		for(int i = 0; i < data.length; i++)
		{
			if( ( i % 8 ) == 0 )
				Mask = data[i];
			else
			{
				if( ( Mask & ( 1 << ( i % 8 ) ) ) == 0 )
					result.put( (byte) (data[i] - 1) );
				else
					result.put( data[i] );
			}
		}

		byte[] array = new byte[result.position()];

		for(int i = 0; i < array.length; i++) {
			array[i] = result.get(i);
		}

		return array;
	}
}
