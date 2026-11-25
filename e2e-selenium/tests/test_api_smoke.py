import requests

def test_api_01_openapi_disponible(api_base_url):
    """API-01 (Alta): GET /v3/api-docs responde 200 con la clave 'openapi'."""
    resp = requests.get(f"{api_base_url}/v3/api-docs", timeout=10)
    assert resp.status_code == 200
    data = resp.json()
    assert "openapi" in data