import os
import time
import pytest
from selenium import webdriver
from selenium.webdriver.chrome.options import Options

@pytest.fixture(scope="session")
def base_url():
    return os.getenv("BASE_URL", "http://localhost:4200").rstrip("/")

@pytest.fixture(scope="session")
def api_base_url(base_url):
    if ":4200" in base_url:
        return base_url.replace(":4200", ":8080")
    return f"{base_url}/api"

@pytest.fixture(scope="session")
def demo_password():
    return os.getenv("E2E_DEMO_PASSWORD", "Demo2026!")

@pytest.fixture(scope="function")
def driver(request):
    options = Options()
    if os.getenv("HEADLESS", "false").lower() == "true":
        options.add_argument("--headless=new")
    options.add_argument("--window-size=1440,900")
    options.add_argument("--no-sandbox")
    options.add_argument("--disable-dev-shm-usage")

    remote_url = os.getenv("SELENIUM_REMOTE_URL")
    if remote_url:
        browser = webdriver.Remote(command_executor=remote_url, options=options)
    else:
        browser = webdriver.Chrome(options=options)

    yield browser

    # Captura automática si el test falla
    if hasattr(request.node, "rep_call") and request.node.rep_call.failed:
        os.makedirs("reports", exist_ok=True)
        nombre = f"reports/fallo_{request.node.name}_{int(time.time())}.png"
        browser.save_screenshot(nombre)

    browser.quit()

@pytest.hookimpl(tryfirst=True, hookwrapper=True)
def pytest_runtest_makereport(item, call):
    outcome = yield
    rep = outcome.get_result()
    setattr(item, "rep_" + rep.when, rep)