/**
 * 
 */
package mybt;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.ardverk.coding.BencodingInputStream;
import org.ardverk.coding.BencodingOutputStream;

/**
 * @author wxy
 *
 */
public class Utils {

	public static byte[] randomId() {
		byte[] s = getRandomString(10);
		MessageDigest messageDigest = null;
		try {
			messageDigest = MessageDigest.getInstance("SHA-1");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		messageDigest.update(s);
		return messageDigest.digest();
	}

	public static byte[] getRandomString(int size) {
		Random random = new Random();
		byte[] b = new byte[size];
		for (int i = 0; i < size; i++) {
			int randomNum = random.nextInt(256);
			b[i] = (byte) randomNum;
		}

		return b;
	}

	public static byte[] getNeighbor(byte[] nodeid, byte[] targetid) {
		byte[] bytes = new byte[20];
		System.arraycopy(targetid, 0, bytes, 0, 8);
		System.arraycopy(nodeid, 8, bytes, 8, 12);
		return bytes;
	}

	public static byte[] enBencode(Map<String, Object> map) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		BencodingOutputStream bencoder = new BencodingOutputStream(out);
		bencoder.writeMap(map);
		byte[] result=out.toByteArray();
		bencoder.close();
		out.close();
		return result;
	}

	@SuppressWarnings("unchecked")
	public static Map<String,Object> deBencode(byte[] data) throws IOException {
		ByteArrayInputStream in = new ByteArrayInputStream(data);
		BencodingInputStream bencode = new BencodingInputStream(in);
		try {
			Map<String,Object> dict = (Map<String, Object>) bencode.readMap();
			bencode.close();
			in.close();
			return dict;
		} catch (Exception e) {
			e.printStackTrace();
			return new HashMap<>();
		}

	}

	public static List<Node> decodeNodes(byte[] bytes) throws UnknownHostException {
		List<Node> result = new ArrayList<>();
		if (bytes == null || "".equals(new String(bytes))) {
			return result;
		}

		int size = bytes.length;

		if (size % 26 != 0) {
			return result;
		}

		for (int i = 0; i < size; i += 26) {
			byte[] currentNodeId = getByteArray(bytes, i, i + 19);
			byte[] currentNodeIp = getByteArray(bytes, i + 20, i + 23);
			byte[] currentNodePort = getByteArray(bytes, i + 24, i + 25);

			int port = getPort(currentNodePort);
			String ip = InetAddress.getByAddress(currentNodeIp).getHostAddress();

			Node n = new Node(ip, port, currentNodeId);
			result.add(n);
		}

		return result;
	}

	public static byte[] getByteArray(byte[] bytes, int start, int end) {
		byte[] newByteArray = new byte[end - start + 1];

		for (int i = start; i <= end; i++) {
			newByteArray[i - start] = bytes[i];
		}

		return newByteArray;
	}

	private static int getPort(byte[] bytes) {
		return ((bytes[0] << 8) & 0x0000ff00) | (bytes[1] & 0x000000ff);
	}
	
	
	/**
	 * byte数组转16进制字符串
	 * 
	 * @param bytes	待转换byte数组
	 * @return		16进制字符串
	 */
    public static String byteArrayToHex(byte[] bytes) {
        return byteArrayToHex(bytes, false);
    }


    /**
     * byte数组转16进制字符串
     * 
     * @param bytes	待转换byte数组
     * @param is
     * @return		16进制字符串
     */
    private static String byteArrayToHex(byte[] bytes, boolean is) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }

        StringBuffer sb = new StringBuffer(bytes.length * 2);
        String hexNumber;
        for (int x = 0; x < bytes.length; x++) {
            hexNumber = "0" + Integer.toHexString(0xff & bytes[x]);

            if (is)
                sb.append("%");
            sb.append(hexNumber.substring(hexNumber.length() - 2));
        }
        return sb.toString();
    }
}
