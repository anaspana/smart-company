from locust import HttpUser, task, between
import random
import string


class SmartCompanyUser(HttpUser):
    wait_time = between(2, 5)
    host = "http://127.0.0.1"

    def on_start(self):
        self.login()

    def login(self):
        response = self.client.post("/api/login", json={
            "username": "andro",
            "password": "sifra"
        })
        if response.status_code == 200:
            self.token = response.json().get("token")
            self.headers = {"Authorization": f"Bearer {self.token}"}
        else:
            self.token = None
            self.headers = {}

    def authed_request(self, method, url, **kwargs):
        response = method(url, headers=self.headers, **kwargs)
        if response.status_code == 401:
            self.login()
            response = method(url, headers=self.headers, **kwargs)
        return response

    @task(3)
    def check_username(self):
        random_username = ''.join(random.choices(string.ascii_lowercase, k=8))
        self.client.get(f"/api/users/check-username?username={random_username}")

    @task(5)
    def browse_products(self):
        self.client.get("/api/products?page=0&size=12")

    @task(4)
    def get_my_companies(self):
        if self.token:
            self.authed_request(self.client.get, "/api/orders/my-companies")

    @task(1)
    def create_order(self):
        if self.token:
            product_id = random.choice([9, 3, 6, 7, 10]) 
            self.authed_request(self.client.post, "/api/orders", json={
                "companyId": 2,
                "items": [{"productId": product_id, "quantity": random.randint(1, 3)}]
            })

    @task(6)
    def get_presigned_url(self):
        self.client.get("/api/files/presigned-url/get?key=product-images/placeholder.pdf")


class WarehouseManagerUser(HttpUser):
    wait_time = between(2, 5)
    host = "http://127.0.0.1"

    def on_start(self):
        self.login()

    def login(self):
        response = self.client.post("/api/login", json={
            "username": "admin",
            "password": "sifra"
        })
        if response.status_code == 200:
            self.token = response.json().get("token")
            self.headers = {"Authorization": f"Bearer {self.token}"}
        else:
            self.token = None
            self.headers = {}

    def authed_request(self, method, url, **kwargs):
        response = method(url, headers=self.headers, **kwargs)
        if response.status_code == 401:
            self.login()
            response = method(url, headers=self.headers, **kwargs)
        return response

    @task(4)
    def browse_warehouses(self):
        if self.token:
            self.authed_request(self.client.get, "/api/warehouses?page=0&size=9")

    @task(3)
    def get_warehouse_detail(self):
        if self.token:
            self.authed_request(self.client.get, "/api/warehouses/100")

    @task(1)
    def update_warehouse(self):
        if self.token:
            self.authed_request(self.client.put, "/api/warehouses/100", json={
                "name": "Warehouse Novi Sad",
                "address": "Test Address",
                "countryId": 1,
                "cityId": 1,
                "latitude": 45.25,
                "longitude": 19.83,
                "imageKeys": [],
                "sectors": [{"name": "Sector A"}]
            })

    @task(2)
    def get_warehouse_temperature(self):
        if self.token:
            self.authed_request(self.client.get, "/api/warehouses/100/sectors/99983/temperature?period=WEEK")

class FactoryManagerUser(HttpUser):
    wait_time = between(2, 5)
    host = "http://127.0.0.1"

    def on_start(self):
        self.login()

    def login(self):
        response = self.client.post("/api/login", json={
            "username": "admin",
            "password": "sifra"
        })
        if response.status_code == 200:
            self.token = response.json().get("token")
            self.headers = {"Authorization": f"Bearer {self.token}"}
        else:
            self.token = None
            self.headers = {}

    def authed_request(self, method, url, **kwargs):
        response = method(url, headers=self.headers, **kwargs)
        if response.status_code == 401:
            self.login()
            response = method(url, headers=self.headers, **kwargs)
        return response

    @task(4)
    def browse_factories(self):
        if self.token:
            self.authed_request(self.client.get, "/api/factories?page=0&size=9")

    @task(3)
    def get_factory_detail(self):
        if self.token:
            self.authed_request(self.client.get, "/api/factories/1")

    @task(1)
    def update_factory(self):
        if self.token:
            self.authed_request(self.client.put, "/api/factories/1", json={
                "name": "Factory Novi Sad",
                "address": "Test Address",
                "countryId": 1,
                "cityId": 1,
                "latitude": 45.25,
                "longitude": 19.83,
                "imageKeys": [],
                "productIds": [2, 3, 4]
            })

    @task(2)
    def get_factory_countries(self):
        if self.token:
            self.authed_request(self.client.get, "/api/factories/countries")

    @task(2)
    def get_factory_cities(self):
        if self.token:
            self.authed_request(self.client.get, "/api/factories/cities?countryId=1")

    @task(3)
    def get_production_data(self):
        if self.token:
            self.authed_request(self.client.get, "/api/factories/1/products/2/production?period=WEEK")

    @task(2)
    def browse_managers(self):
        if self.token:
            self.authed_request(self.client.get, "/api/users/managers?search=&page=0&size=9")

    @task(1)
    def update_product(self):
        if self.token:
            self.authed_request(self.client.put, "/api/products/2", json={
                "name": "Guma 195/55/R15-H",
                "description": "Test",
                "price": 49.99,
                "weight": 8.5,
                "image": None,
                "isForSale": True,
                "categoryId": 2
            })

    @task(2)
    def get_categories(self):
        self.client.get("/api/categories")

    @task(2)
    def get_product_detail(self):
        self.client.get("/api/products/2")


class VehicleCompanyUser(HttpUser):
    wait_time = between(2, 5)
    host = "http://127.0.0.1"

    def on_start(self):
        self.login()

    def login(self):
        response = self.client.post("/api/login", json={
            "username": "admin",
            "password": "sifra"
        })
        if response.status_code == 200:
            self.token = response.json().get("token")
            self.headers = {"Authorization": f"Bearer {self.token}"}
        else:
            self.token = None
            self.headers = {}

    def authed_request(self, method, url, **kwargs):
        response = method(url, headers=self.headers, **kwargs)
        if response.status_code == 401:
            self.login()
            response = method(url, headers=self.headers, **kwargs)
        return response

    @task(4)
    def browse_vehicles(self):
        if self.token:
            self.authed_request(self.client.get, "/api/vehicles?page=0&size=9")

    @task(3)
    def get_vehicle_detail(self):
        if self.token:
            self.authed_request(self.client.get, "/api/vehicles/1")

    @task(1)
    def update_vehicle(self):
        if self.token:
            self.authed_request(self.client.put, "/api/vehicles/1", json={
                "licensePlate": "NS-001-AB",
                "weightLimit": 1000.0,
                "brandId": 3,
                "modelId": 10,
                "imageKeys": []
            })

    @task(2)
    def get_vehicle_brands(self):
        if self.token:
            self.authed_request(self.client.get, "/api/vehicles/brands")

    @task(2)
    def get_vehicle_models(self):
        if self.token:
            self.authed_request(self.client.get, "/api/vehicles/models?brandId=3")

    @task(3)
    def get_distance_data(self):
        if self.token:
            self.authed_request(self.client.get, "/api/vehicles/1/distance?period=WEEK")

    @task(3)
    def browse_company_requests(self):
        if self.token:
            self.authed_request(self.client.get, "/api/companies?page=0&size=9")

    @task(2)
    def get_company_detail(self):
        if self.token:
            self.authed_request(self.client.get, "/api/companies/1/manage")

    @task(1)
    def decide_company(self):
        if not self.token:
            return
        resp = self.authed_request(self.client.get, "/api/companies?status=PENDING&page=0&size=1")
        try:
            data = resp.json()
            content = data.get("content", [])
            if not content:
                return
            company_id = content[0]["id"]
        except Exception:
            return

        decision_resp = self.client.put(
            f"/api/companies/{company_id}/decision",
            headers=self.headers,
            json={"approve": True, "rejectionReason": None},
            catch_response=True
        )
        with decision_resp as resp2:
            if resp2.status_code in (200, 400, 409):
                resp2.success()  # "already processed" pod konkurencijom je ocekivano, ne racunaj kao fail
            else:
                resp2.failure(f"Unexpected status {resp2.status_code}")
    
    @task(2)
    def get_company_countries(self):
        if self.token:
            self.authed_request(self.client.get, "/api/companies/countries")