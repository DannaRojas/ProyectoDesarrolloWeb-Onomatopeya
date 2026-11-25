package com.onomatopeya;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class FormularioLoginTest {

    @Test
    void debeMostrarLosCamposDelFormularioLogin() {

        WebDriver driver = new ChromeDriver();

        try {
            driver.get("http://localhost:50135");

            WebElement correo = driver.findElement(By.id("correo"));
            WebElement contrasena = driver.findElement(By.id("contrasena"));
            WebElement boton = driver.findElement(
                By.cssSelector("button[type='submit']")
            );

            assertTrue(correo.isDisplayed());
            assertTrue(correo.isEnabled());

            assertTrue(contrasena.isDisplayed());
            assertTrue(contrasena.isEnabled());

            assertTrue(boton.isDisplayed());
            assertTrue(boton.isEnabled());

            System.out.println("Formulario de inicio de sesión verificado correctamente.");

        } finally {
            driver.quit();
        }
    }
}
