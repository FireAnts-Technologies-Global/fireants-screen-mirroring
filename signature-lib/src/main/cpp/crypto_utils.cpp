#include "signature_native.h"
#include <sstream>
#include <iomanip>
#include <cstring>
#include <vector>
#include <cstdlib>

// Simple SHA-256 implementation (no external dependencies)
// Based on FIPS 180-4 specification

namespace {
    // SHA-256 constants
    const uint32_t K[64] = {
        0x428a2f98, 0x71374491, 0xb5c0fbcf, 0xe9b5dba5,
        0x3956c25b, 0x59f111f1, 0x923f82a4, 0xab1c5ed5,
        0xd807aa98, 0x12835b01, 0x243185be, 0x550c7dc3,
        0x72be5d74, 0x80deb1fe, 0x9bdc06a7, 0xc19bf174,
        0xe49b69c1, 0xefbe4786, 0x0fc19dc6, 0x240ca1cc,
        0x2de92c6f, 0x4a7484aa, 0x5cb0a9dc, 0x76f988da,
        0x983e5152, 0xa831c66d, 0xb00327c8, 0xbf597fc7,
        0xc6e00bf3, 0xd5a79147, 0x06ca6351, 0x14292967,
        0x27b70a85, 0x2e1b2138, 0x4d2c6dfc, 0x53380d13,
        0x650a7354, 0x766a0abb, 0x81c2c92e, 0x92722c85,
        0xa2bfe8a1, 0xa81a664b, 0xc24b8b70, 0xc76c51a3,
        0xd192e819, 0xd6990624, 0xf40e3585, 0x106aa070,
        0x19a4c116, 0x1e376c08, 0x2748774c, 0x34b0bcb5,
        0x391c0cb3, 0x4ed8aa4a, 0x5b9cca4f, 0x682e6ff3,
        0x748f82ee, 0x78a5636f, 0x84c87814, 0x8cc70208,
        0x90befffa, 0xa4506ceb, 0xbef9a3f7, 0xc67178f2
    };
    
    inline uint32_t rotr(uint32_t x, uint32_t n) {
        return (x >> n) | (x << (32 - n));
    }
    
    inline uint32_t ch(uint32_t x, uint32_t y, uint32_t z) {
        return (x & y) ^ (~x & z);
    }
    
    inline uint32_t maj(uint32_t x, uint32_t y, uint32_t z) {
        return (x & y) ^ (x & z) ^ (y & z);
    }
    
    inline uint32_t sigma0(uint32_t x) {
        return rotr(x, 2) ^ rotr(x, 13) ^ rotr(x, 22);
    }
    
    inline uint32_t sigma1(uint32_t x) {
        return rotr(x, 6) ^ rotr(x, 11) ^ rotr(x, 25);
    }
    
    inline uint32_t gamma0(uint32_t x) {
        return rotr(x, 7) ^ rotr(x, 18) ^ (x >> 3);
    }
    
    inline uint32_t gamma1(uint32_t x) {
        return rotr(x, 17) ^ rotr(x, 19) ^ (x >> 10);
    }
    
    void sha256_transform(uint32_t state[8], const uint8_t block[64]) {
        uint32_t W[64];
        uint32_t a, b, c, d, e, f, g, h, t1, t2;

        for (int i = 0; i < 16; i++) {
            W[i] = (block[i * 4] << 24) | (block[i * 4 + 1] << 16) |
                   (block[i * 4 + 2] << 8) | block[i * 4 + 3];
        }
        
        for (int i = 16; i < 64; i++) {
            W[i] = gamma1(W[i - 2]) + W[i - 7] + gamma0(W[i - 15]) + W[i - 16];
        }

        a = state[0];
        b = state[1];
        c = state[2];
        d = state[3];
        e = state[4];
        f = state[5];
        g = state[6];
        h = state[7];

        for (int i = 0; i < 64; i++) {
            t1 = h + sigma1(e) + ch(e, f, g) + K[i] + W[i];
            t2 = sigma0(a) + maj(a, b, c);
            h = g;
            g = f;
            f = e;
            e = d + t1;
            d = c;
            c = b;
            b = a;
            a = t1 + t2;
        }

        state[0] += a;
        state[1] += b;
        state[2] += c;
        state[3] += d;
        state[4] += e;
        state[5] += f;
        state[6] += g;
        state[7] += h;
    }
}

std::string calculateSHA256(const uint8_t* data, size_t length) {
    uint32_t state[8] = {
        0x6a09e667, 0xbb67ae85, 0x3c6ef372, 0xa54ff53a,
        0x510e527f, 0x9b05688c, 0x1f83d9ab, 0x5be0cd19
    };
    
    uint8_t block[64];
    size_t i = 0;

    while (i + 64 <= length) {
        std::memcpy(block, data + i, 64);
        sha256_transform(state, block);
        i += 64;
    }

    size_t remaining = length - i;
    std::memcpy(block, data + i, remaining);
    block[remaining] = 0x80;
    
    if (remaining >= 56) {
        std::memset(block + remaining + 1, 0, 64 - remaining - 1);
        sha256_transform(state, block);
        std::memset(block, 0, 56);
    } else {
        std::memset(block + remaining + 1, 0, 56 - remaining - 1);
    }

    uint64_t bitLength = length * 8;
    for (int j = 0; j < 8; j++) {
        block[63 - j] = bitLength & 0xff;
        bitLength >>= 8;
    }
    
    sha256_transform(state, block);

    std::stringstream ss;
    for (int i = 0; i < 8; i++) {
        ss << std::hex << std::setw(8) << std::setfill('0') << state[i];
    }
    
    return ss.str();
}

std::string formatHashWithColons(const std::string& hash) {
    std::string result;
    result.reserve(hash.length() + hash.length() / 2);
    
    for (size_t i = 0; i < hash.length(); i += 2) {
        if (i > 0) {
            result += ':';
        }
        result += std::toupper(hash[i]);
        result += std::toupper(hash[i + 1]);
    }
    
    return result;
}

std::string calculateHMACSHA256(const std::string& key, const std::string& value) {
    const size_t BLOCK_SIZE = 64;

    std::vector<uint8_t> keyBytes;
    
    if (key.length() > BLOCK_SIZE) {
        std::string hashedKey = calculateSHA256(
            reinterpret_cast<const uint8_t*>(key.c_str()), key.length());

        for (size_t i = 0; i < hashedKey.length(); i += 2) {
            std::string byteStr = hashedKey.substr(i, 2);
            uint8_t byte = static_cast<uint8_t>(std::strtol(byteStr.c_str(), nullptr, 16));
            keyBytes.push_back(byte);
        }
    } else {
        keyBytes.assign(key.begin(), key.end());
    }

    keyBytes.resize(BLOCK_SIZE, 0);

    std::vector<uint8_t> innerKey(BLOCK_SIZE);
    std::vector<uint8_t> outerKey(BLOCK_SIZE);
    
    for (size_t i = 0; i < BLOCK_SIZE; i++) {
        innerKey[i] = keyBytes[i] ^ 0x36;
        outerKey[i] = keyBytes[i] ^ 0x5c;
    }

    std::vector<uint8_t> innerData;
    innerData.insert(innerData.end(), innerKey.begin(), innerKey.end());
    innerData.insert(innerData.end(), value.begin(), value.end());
    
    std::string innerHash = calculateSHA256(innerData.data(), innerData.size());

    std::vector<uint8_t> innerHashBytes;
    for (size_t i = 0; i < innerHash.length(); i += 2) {
        std::string byteStr = innerHash.substr(i, 2);
        uint8_t byte = static_cast<uint8_t>(std::strtol(byteStr.c_str(), nullptr, 16));
        innerHashBytes.push_back(byte);
    }

    std::vector<uint8_t> outerData;
    outerData.insert(outerData.end(), outerKey.begin(), outerKey.end());
    outerData.insert(outerData.end(), innerHashBytes.begin(), innerHashBytes.end());
    
    return calculateSHA256(outerData.data(), outerData.size());
}
