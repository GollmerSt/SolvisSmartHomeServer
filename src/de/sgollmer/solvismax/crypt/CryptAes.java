/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.crypt;

import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.Base64.Encoder;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import de.sgollmer.solvismax.Constants;

public class CryptAes {

	private byte[] v = null;

	private static int[] primes = new int[] { 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37, 41, 43, 47, 53, 59, 61, 67, 71,
			73, 79, 83, 89, 97, 101, 103, 107, 109, 113, 127, 131, 137, 139, 149, 151, 157, 163, 167, 173, 179, 181,
			191, 193, 197, 199, 211, 223, 227, 229, 233, 239, 241, 251, 257, 263, 269, 271, 277, 281, 283, 293, 307,
			311, 313, 317, 331, 337, 347, 349, 353, 359, 367, 373, 379, 383, 389, 397, 401, 409, 419, 421, 431, 433,
			439, 443, 449, 457, 461, 463, 467, 479, 487, 491, 499, 503, 509, 521, 523, 541, 547, 557, 563, 569, 571,
			577, 587, 593, 599, 601, 607, 613, 617, 619, 631, 641, 643, 647, 653, 659, 661, 673, 677, 683, 691, 701,
			709, 719, 727, 733, 739, 743, 751, 757, 761, 769, 773, 787, 797, 809, 811, 821, 823, 827, 829, 839, 853,
			857, 859, 863, 877, 881, 883, 887, 907, 911, 919, 929, 937, 941, 947, 953, 967, 971, 977, 983, 991, 997 };

	private static byte[] cK() {
		byte[] result = { (byte) (23 * 17), (byte) (409 * 337), (byte) (293 * 647), (byte) (601 * 145 + 2404),
				(byte) (643 * 52 + 643), (byte) (191 * 571), (byte) (677 * 613), (byte) (971 * 43), (byte) (653 * 97),
				(byte) (733 * 997), (byte) (157 * 83), (byte) (307 * 569), (byte) (389 * 613), (byte) (293 * 277),
				(byte) (617 * 349), (byte) (727 * 439) };
		return result;
	}

	public String encrypt(String word) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException,
			IllegalBlockSizeException, BadPaddingException {
		Key aesKey = new SecretKeySpec(CryptAes.cK(), "AES");
		Cipher cipher = Cipher.getInstance("AES");
		cipher.init(Cipher.ENCRYPT_MODE, aesKey);
		this.cI(word);
		byte[] encrypted = cipher.doFinal(this.v);
		Encoder base64Encoder = Base64.getEncoder();
		return base64Encoder.encodeToString(encrypted);
	}

	public void decrypt(String cryptedString) throws NoSuchAlgorithmException, NoSuchPaddingException,
			InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
		Decoder base64Decoder = Base64.getDecoder();
		byte[] crypted = base64Decoder.decode(cryptedString);
		Key aesKey = new SecretKeySpec(CryptAes.cK(), "AES");
		Cipher cipher = Cipher.getInstance("AES");
		cipher.init(Cipher.DECRYPT_MODE, aesKey);
		this.v = cipher.doFinal(crypted);
	}

	private void cI(String word) {
		byte[] bb = word.getBytes();
		this.v = new byte[(bb.length + 2 + 16) / 16 * 16 - 1];
		this.v[0] = (byte) (bb.length & 0xff);
		this.v[1] = (byte) ((bb.length >> 8) & 0xff);
		for (int i = 0; i < bb.length; ++i) {
			this.v[i + 2] = bb[i];
		}
		int previous = Constants.CRYPT_PREVIOUS;
		for (int i = 0; i < this.v.length; ++i) {
			byte b = this.v[i];
			byte x = (byte) primes[previous % primes.length];
			byte c = (byte) ((b + previous) ^ x);
			this.v[i] = c;
			previous = c & 0xff;
		}
	}

	public char[] cP() {
		byte[] result = null;
		if (this.v != null) {
			result = new byte[this.v.length];
			int previous = Constants.CRYPT_PREVIOUS;
			for (int i = 0; i < this.v.length; ++i) {
				byte c = this.v[i];
				byte x = (byte) primes[previous % primes.length];
				byte b = (byte) ((c ^ x) - previous);
				result[i] = b;
				previous = c & 0xff;
			}
		}
		int length = (result[0] & 0xff) | (result[1] << 8 & 0xff);
		return new String(Arrays.copyOfRange(result, 2, length + 2)).toCharArray();
	}

	public void set(String value) {
		if (this.v == null) {
			this.cI(value);
		}
	}

	public static void main(String[] args) throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException,
			IllegalBlockSizeException, BadPaddingException {
		CryptAes aes = new CryptAes();
		for (int i = 0; i < 100; ++i) {
			StringBuilder builder = new StringBuilder();
			for (int j = 1; j < i + 2; ++j) {
				builder.append(Integer.toString(j % 10));
			}
			String uncrypted = builder.toString();
			String crypted = aes.encrypt(uncrypted);
			aes.decrypt(crypted);
			char[] u = aes.cP();
			String check = new String(u);
			if (!check.equals(uncrypted)) {
				System.err.println("Failing on " + uncrypted);
			}
		}
	}

}
