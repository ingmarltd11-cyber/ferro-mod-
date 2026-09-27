package dev.ferro.client.utils;

import java.nio.charset.StandardCharsets;

/**
 * A string that is not stored in plain text inside the compiled jar.
 *
 * <p>The UTF-8 bytes are XOR-ed with a fixed key mixed with a per-instance salt. This is obfuscation,
 * not cryptography: it stops casual string dumps of the jar from revealing module names and chat
 * messages, which is the only guarantee it makes.</p>
 */
public final class EncryptedString implements CharSequence {

    private static final byte[] KEY = {
            0x46, 0x45, 0x52, 0x52, 0x4F, 0x2D, 0x43, 0x4C, 0x49, 0x45, 0x4E, 0x54, 0x2D, 0x31, 0x32, 0x31, 0x31
    };

    private final byte[] data;
    private final int salt;
    private String cache;

    private EncryptedString(byte[] data, int salt) {
        this.data = data;
        this.salt = salt;
    }

    /**
     * Wraps a literal string.
     *
     * @param plain the plain text value
     * @return the obfuscated wrapper
     */
    public static EncryptedString of(String plain) {
        byte[] raw = plain.getBytes(StandardCharsets.UTF_8);
        int salt = Math.abs(plain.hashCode() % 251) + 1;
        byte[] encrypted = new byte[raw.length];
        for (int index = 0; index < raw.length; index++) {
            encrypted[index] = (byte) (raw[index] ^ KEY[(index + salt) % KEY.length] ^ salt);
        }
        return new EncryptedString(encrypted, salt);
    }

    /**
     * @return the decrypted value, cached after the first call
     */
    public String getValue() {
        if (cache == null) {
            byte[] raw = new byte[data.length];
            for (int index = 0; index < data.length; index++) {
                raw[index] = (byte) (data[index] ^ KEY[(index + salt) % KEY.length] ^ salt);
            }
            cache = new String(raw, StandardCharsets.UTF_8);
        }
        return cache;
    }

    @Override
    public int length() {
        return getValue().length();
    }

    @Override
    public char charAt(int index) {
        return getValue().charAt(index);
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return getValue().subSequence(start, end);
    }

    @Override
    public String toString() {
        return getValue();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof EncryptedString encryptedString)) {
            return false;
        }
        return getValue().equals(encryptedString.getValue());
    }

    @Override
    public int hashCode() {
        return getValue().hashCode();
    }
}
