package br.pucrs.pulsepass.checkout;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {"br.pucrs.pulsepass.checkout", "br.pucrs.pulsepass.commons"})
@EnableScheduling
public class CheckoutApplication {

  public static void main(String[] args) {
    SpringApplication.run(CheckoutApplication.class, args);
  }
}
