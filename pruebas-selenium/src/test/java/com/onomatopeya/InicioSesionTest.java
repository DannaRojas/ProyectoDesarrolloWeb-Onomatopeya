package com.onomatopeya;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class InicioSesionTest {

    @Test
    void debeAbrirLaPantallaDeInicioSesion() {

        WebDriver driver = new ChromeDriver();

        try {
            driver.get("http://localhost:50135");

            String titulo = driver.getTitle();

            System.out.println("Título de la página: " + titulo);

            assertTrue(
                driver.getCurrentUrl().contains("localhost:50135"),
                "La aplicación debe abrir correctamente"
            );

            assertTrue(
                titulo != null && !titulo.isBlank(),
                "La página debe tener un título"
            );

        } finally {
            driver.quit();
        }
    }
}
