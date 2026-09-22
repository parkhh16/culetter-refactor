package com.sim.backend.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.TransactionManager;

@Configuration
public class Web3Config {

  @Bean
  Web3j web3j(@Value("${eth.rpc-url}") String rpcUrl) {
    return Web3j.build(new HttpService(rpcUrl));
  }

  /** 개인 키로 서명 키 적용 */
  @Bean
  Credentials credentials(@Value("${eth.private-key}") String pk) {
    return Credentials.create("0x" + pk);
  }

  /** 트랜잭션을 주어진 체인에 날릴 예정 */
  @Bean
  TransactionManager txManager(Web3j web3j, Credentials credentials,
                               @Value("${eth.chain-id}") long chainId) {
    return new RawTransactionManager(web3j, credentials, chainId);
  }

//  @Bean
//  CommandLineRunner verify(Web3j web3j, @Value("${eth.chain-id}") long expected) {
//    return args -> {
//      var res = web3j.ethChainId().send();
//      System.out.println("[ETH] chainId=" + res.getChainId().longValue() + " (expected=" + expected + ")");
//
//      var balWei = web3j.ethGetBalance("0xAc0e7E81AedAAC29A5DE28b6a6DB14010fC77711",
//              org.web3j.protocol.core.DefaultBlockParameterName.LATEST).send().getBalance();
//      BigDecimal eth = Convert.fromWei(balWei.toString(), Convert.Unit.ETHER);
//      System.out.println("[ETH] balance(ETH)=" + eth.toPlainString());
//    };
//  }
//
//  @Bean
//  ContractGasProvider gasProvider(Web3j web3j) {
//    try {
//      var gasPrice = web3j.ethGasPrice().send().getGasPrice();
//      return new StaticGasProvider(gasPrice, BigInteger.valueOf(600_000)); // 가스리밋은 함수에 맞춰 조정
//    } catch (Exception e) {
//      return new StaticGasProvider(BigInteger.valueOf(2_000_000_000L), BigInteger.valueOf(600_000));
//    }
//  }
}