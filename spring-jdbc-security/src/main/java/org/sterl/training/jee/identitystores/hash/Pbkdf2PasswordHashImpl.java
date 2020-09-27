/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015-2017 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://oss.oracle.com/licenses/CDDL+GPL-1.1
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package org.sterl.training.jee.identitystores.hash;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import static java.util.Arrays.asList;
import java.util.Base64;
import static java.util.Collections.unmodifiableSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * This interface represents the built-in {@code Pbkdf2PasswordHash} implementation.
 * <p>
 * To use {@code Pbkdf2PasswordHash} with the built-in Database {@link IdentityStore},
 * configure this interface type as the {@code hashAlgorithm} value
 * on the {@link DatabaseIdentityStoreDefinition} annotation.
 * <p>
 * To configure parameters for {@code Pbkdf2PasswordHash}, specify them as the
 * {@code hashAlgorithmParameters} value on the {@link DatabaseIdentityStoreDefinition} annotation.
 * <p>
 * The built-in implementation must support the following configurable parameters:
 * <blockquote><pre>
Pbkdf2PasswordHash.Algorithm      // default "PBKDF2WithHmacSHA256"
Pbkdf2PasswordHash.Iterations     // default 2048, minimum 1024
Pbkdf2PasswordHash.SaltSizeBytes  // default 32, minimum 16
Pbkdf2PasswordHash.KeySizeBytes   // default 32, minimum 16
 * </pre></blockquote>
 * <p>
 * And the following PBKDF2 algorithms:
 * <blockquote><pre>
PBKDF2WithHmacSHA224
PBKDF2WithHmacSHA256
PBKDF2WithHmacSHA384
PBKDF2WithHmacSHA512
 * </pre></blockquote>
 * Algorithm names are the string literal names documented for the corresponding algorithms by the
 * <a href="https://docs.oracle.com/javase/8/docs/technotes/guides/security/StandardNames.html" target="_top">
Java Cryptography Architecture Standard Algorithm Name Documentation</a>.
 * <p>
 * The encoded format produced by {@link #generate(char[])}, and consumed by {@link #verify(char[], String)},
 * is as follows:
 * <blockquote><pre>
{@code <algorithm>:<iterations>:<base64(salt)>:<base64(hash)>}
 * </pre></blockquote>
 * Where:
 * <ul>
 * <li><i>algorithm</i> -- the algorithm used to generate the hash
 * <li><i>iterations</i> -- the number of iterations used to generate the hash
 * <li><i>base64(salt)</i> -- the salt used to generate the hash, base64-encoded
 * <li><i>base64(hash)</i> -- the hash value, base64-encoded
 * </ul>
 * <p>
 * Because the algorithm and the parameters used to generate the hash are stored with the hash,
 * the built-in {@code Pbkdf2PasswordHash} implementation can verify hashes generated using algorithm
 * and parameter values that differ from the currently configured values. This means the configuration
 * parameters can be changed without impacting the ability to verify existing password hashes.
 * <p>
 * (Password hashes generated using algorithms/parameters outside the range supported by
 * {@code Pbkdf2PasswordHash} cannot be verified.)
 *
 * @see DatabaseIdentityStoreDefinition#hashAlgorithm()
 * @see DatabaseIdentityStoreDefinition#hashAlgorithmParameters()
 * @link https://github.com/payara/patched-src-security-soteria/blob/master/impl/src/main/java/org/glassfish/soteria/identitystores/hash/Pbkdf2PasswordHashImpl.java
 */
public class Pbkdf2PasswordHashImpl implements PasswordEncoder {

    private static final Set<String> SUPPORTED_ALGORITHMS = unmodifiableSet(new HashSet<>(asList(
            "PBKDF2WithHmacSHA224",
            "PBKDF2WithHmacSHA256",
            "PBKDF2WithHmacSHA384",
            "PBKDF2WithHmacSHA512"
            )));

    private static final String DEFAULT_ALGORITHM = "PBKDF2WithHmacSHA256";

    private static final int DEFAULT_ITERATIONS = 2048;
    private static final int DEFAULT_SALT_SIZE  = 32;         // 32-byte/256-bit salt
    private static final int DEFAULT_KEY_SIZE   = 32;         // 32-byte/256-bit key/hash

    private static final int MIN_ITERATIONS = 1024;
    private static final int MIN_SALT_SIZE  = 16;             // 16-byte/128-bit minimum salt
    private static final int MIN_KEY_SIZE   = 16;             // 16-byte/128-bit minimum key/hash

    private static final String PROPERTY_ALGORITHM  = "Pbkdf2PasswordHash.Algorithm";
    private static final String PROPERTY_ITERATIONS = "Pbkdf2PasswordHash.Iterations";
    private static final String PROPERTY_SALTSIZE   = "Pbkdf2PasswordHash.SaltSizeBytes";
    private static final String PROPERTY_KEYSIZE    = "Pbkdf2PasswordHash.KeySizeBytes";

    private String configuredAlgorithm  = DEFAULT_ALGORITHM;   // PBKDF2 algorithm to use
    private int configuredIterations    = DEFAULT_ITERATIONS;  // number of iterations
    private int configuredSaltSizeBytes = DEFAULT_SALT_SIZE;   // salt size in bytes
    private int configuredKeySizeBytes  = DEFAULT_KEY_SIZE;    // derived key (i.e., password hash) size in bytes
    
    private final SecureRandom random = new SecureRandom();

    public void initialize(Map<String, String> parameters) {
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            if (entry.getKey().equals(PROPERTY_ALGORITHM)) {
                if (!SUPPORTED_ALGORITHMS.contains(entry.getValue())) {
                    throw new IllegalArgumentException("Bad Algorithm parameter: " + entry.getValue());
                }
                configuredAlgorithm = entry.getValue();
            }
            else if (entry.getKey().equals(PROPERTY_ITERATIONS)) {
                try {
                    configuredIterations = Integer.parseInt(entry.getValue());
                }
                catch (Exception e) {
                    throw new IllegalArgumentException("Bad Iterations parameter: " + entry.getValue());
                }
                if (configuredIterations < MIN_ITERATIONS) {
                    throw new IllegalArgumentException("Bad Iterations parameter: " + entry.getValue());
                }
            }
            else if (entry.getKey().equals(PROPERTY_SALTSIZE)) {
                try {
                    configuredSaltSizeBytes = Integer.parseInt(entry.getValue());
                }
                catch (Exception e) {
                    throw new IllegalArgumentException("Bad SaltSizeBytes parameter: " + entry.getValue());
                }
                if (configuredSaltSizeBytes < MIN_SALT_SIZE) {
                    throw new IllegalArgumentException("Bad SaltSizeBytes parameter: " + entry.getValue());
                }
            }
            else if (entry.getKey().equals(PROPERTY_KEYSIZE)) {
                try {
                    configuredKeySizeBytes = Integer.parseInt(entry.getValue());
                }
                catch (Exception e) {
                    throw new IllegalArgumentException("Bad KeySizeBytes parameter: " + entry.getValue());
                }
                if (configuredKeySizeBytes < MIN_KEY_SIZE) {
                    throw new IllegalArgumentException("Bad KeySizeBytes parameter: " + entry.getValue());
                }
            }
            else {
                throw new IllegalArgumentException("Unrecognized parameter for Pbkdf2PasswordHash");
            }
        }
    }
    

    @Override
    public String encode(CharSequence rawPassword) {
        return generate(rawPassword.toString().toCharArray());
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        return verify(rawPassword.toString().toCharArray(), encodedPassword);
    }

    public String generate(char[] password) {
        byte[] salt = getRandomSalt(new byte[configuredSaltSizeBytes]);
        byte[] hash = pbkdf2(password, salt, configuredAlgorithm, configuredIterations, configuredKeySizeBytes);
        return new EncodedPasswordHash(hash, salt, configuredAlgorithm, configuredIterations).getEncoded();
    }

    public boolean verify(char[] password, String hashedPassword) {
        EncodedPasswordHash encodedPasswordHash = new EncodedPasswordHash(hashedPassword);
        byte[] hashToVerify = pbkdf2(
                password,
                encodedPasswordHash.getSalt(),
                encodedPasswordHash.getAlgorithm(),
                encodedPasswordHash.getIterations(),
                encodedPasswordHash.getHash().length);
        return MessageDigest.isEqual(hashToVerify, encodedPasswordHash.getHash());
    }

    private byte[] pbkdf2(char[] password, byte[] salt, String algorithm, int iterations, int keySizeBytes) {
        try {
            return SecretKeyFactory.getInstance(algorithm).generateSecret(
                    new PBEKeySpec(password, salt, iterations, keySizeBytes * 8)).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException(e);
        }
    }

    private synchronized byte[] getRandomSalt(byte[] salt) {
        random.nextBytes(salt);
        return salt;
    }

    private static class EncodedPasswordHash {

        private String algorithm;
        private int iterations;
        private byte[] salt;
        private byte[] hash;
        private String encoded;

        private EncodedPasswordHash() {};

        EncodedPasswordHash(byte[] hash, byte[] salt, String algorithm, int iterations) {
            this.algorithm = algorithm;
            this.iterations = iterations;
            this.salt = salt;
            this.hash = hash;
            encode();
        }

        EncodedPasswordHash(String encoded) {
            this.encoded = encoded;
            decode();
        }

        String getAlgorithm() { return algorithm; }
        int getIterations() { return iterations; }
        byte[] getSalt() { return salt; }
        byte[] getHash() { return hash; }
        String getEncoded() { return encoded; }

        private void encode() {
            encoded = algorithm + ":" + iterations + ":" +
                    Base64.getEncoder().encodeToString(salt) + ":" +
                    Base64.getEncoder().encodeToString(hash);
        }

        private void decode() {
            String[] tokens = encoded.split(":");
            if (tokens.length != 4) {
                throw new IllegalArgumentException("Bad hash encoding");
            }
            if (!SUPPORTED_ALGORITHMS.contains(tokens[0])) {
                throw new IllegalArgumentException("Bad hash encoding");
            }
            algorithm = tokens[0];
            try {
                iterations = Integer.parseInt(tokens[1]);
                salt = Base64.getDecoder().decode(tokens[2]);
                hash = Base64.getDecoder().decode(tokens[3]);
            }
            catch (Exception e) {
                throw new IllegalArgumentException("Bad hash encoding", e);
            }
        }
    }
}