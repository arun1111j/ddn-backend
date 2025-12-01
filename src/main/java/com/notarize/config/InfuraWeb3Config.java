package com.notarize.config;

import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.gas.StaticGasProvider;

import java.math.BigInteger;
import java.util.concurrent.TimeUnit;

@Slf4j
@Configuration
public class InfuraWeb3Config {

    @Value("${web3j.client-url}")
    private String infuraUrl;

    @Value("${web3j.private-key}")
    private String privateKey;

    @Value("${web3j.gas-price:20000000000}")
    private String gasPrice;

    @Value("${web3j.gas-limit:6000000}")
    private String gasLimit;

    @Bean
    public Web3j web3j() {
        log.info("🔗 Connecting to Infura: {}", maskUrl(infuraUrl));

        // Create OkHttpClient with timeouts for Infura
        OkHttpClient httpClient = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();

        HttpService httpService = new HttpService(infuraUrl, httpClient);
        Web3j web3j = Web3j.build(httpService);

        try {
            // Test connection
            String clientVersion = web3j.web3ClientVersion().send().getWeb3ClientVersion();
            log.info("✅ Connected to Ethereum network: {}", clientVersion);

            // Get network ID
            String networkId = web3j.netVersion().send().getNetVersion();
            log.info("📡 Network ID: {} ({})", networkId, getNetworkName(networkId));

            // Get latest block
            BigInteger blockNumber = web3j.ethBlockNumber().send().getBlockNumber();
            log.info("🔢 Latest block: {}", blockNumber);

        } catch (Exception e) {
            log.error("❌ Failed to connect to Infura: {}", e.getMessage());
            throw new RuntimeException("Cannot connect to Infura", e);
        }

        return web3j;
    }

    @Bean
    public Credentials credentials() {
        try {
            log.info("🔐 Loading wallet credentials...");
            Credentials creds = Credentials.create(privateKey);
            log.info("✅ Wallet loaded: {}", maskAddress(creds.getAddress()));
            return creds;
        } catch (Exception e) {
            log.error("❌ Failed to load credentials: {}", e.getMessage());
            throw new RuntimeException("Invalid private key", e);
        }
    }

    @Bean
    public StaticGasProvider gasProvider() {
        BigInteger price = new BigInteger(gasPrice);
        BigInteger limit = new BigInteger(gasLimit);

        log.info("⛽ Gas configuration - Price: {} wei, Limit: {}", price, limit);

        return new StaticGasProvider(price, limit);
    }

    // Helper methods
    private String maskUrl(String url) {
        if (url == null || url.isEmpty())
            return "***";
        int lastSlash = url.lastIndexOf('/');
        if (lastSlash > 0 && lastSlash < url.length() - 1) {
            String projectId = url.substring(lastSlash + 1);
            if (projectId.length() > 8) {
                String masked = projectId.substring(0, 4) + "..." + projectId.substring(projectId.length() - 4);
                return url.substring(0, lastSlash + 1) + masked;
            }
        }
        return url;
    }

    private String maskAddress(String address) {
        if (address == null || address.length() < 10)
            return "***";
        return address.substring(0, 6) + "..." + address.substring(address.length() - 4);
    }

    private String getNetworkName(String networkId) {
        return switch (networkId) {
            case "1" -> "Mainnet";
            case "11155111" -> "Sepolia Testnet";
            case "5" -> "Goerli Testnet";
            case "80001" -> "Mumbai (Polygon)";
            case "137" -> "Polygon Mainnet";
            default -> "Unknown Network";
        };
    }
}