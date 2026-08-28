import json
import os
import uuid
from http.server import BaseHTTPRequestHandler, HTTPServer
from datetime import datetime, timezone
from auth import get_db, hash_password, verify_password, create_session, get_user_from_token

from urllib.parse import urlparse

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
DATA_DIR = os.path.join(BASE_DIR, "data")
DATA_FILE = os.path.join(DATA_DIR, "apps.json")

os.makedirs(DATA_DIR, exist_ok=True)

if not os.path.exists(DATA_FILE):
    with open(DATA_FILE, "w", encoding="utf-8") as f:
        json.dump([], f, ensure_ascii=False, indent=2)


def load_apps():
    try:
        with open(DATA_FILE, "r", encoding="utf-8") as f:
            return json.load(f)
    except (FileNotFoundError, json.JSONDecodeError):
        return []


def save_apps(apps):
    temp_file = DATA_FILE + ".tmp"

    with open(temp_file, "w", encoding="utf-8") as f:
        json.dump(apps, f, ensure_ascii=False, indent=2)

    os.replace(temp_file, DATA_FILE)


class StoreAPI(BaseHTTPRequestHandler):

    def send_json(self, data, status=200):
        body = json.dumps(
            data,
            ensure_ascii=False
        ).encode("utf-8")

        self.send_response(status)
        self.send_header(
            "Content-Type",
            "application/json; charset=utf-8"
        )
        self.send_header(
            "Content-Length",
            str(len(body))
        )
        self.send_header(
            "Access-Control-Allow-Origin",
            "*"
        )
        self.end_headers()

        self.wfile.write(body)

    def do_GET(self):

        path = urlparse(self.path).path

        if path == "/api/health":
            self.send_json({
                "success": True,
                "service": "SHIVAM STORE API",
                "status": "online"
            })
            return

        if path == "/api/apps":
            apps = load_apps()

            self.send_json({
                "success": True,
                "count": len(apps),
                "apps": apps
            })
            return

        if path.startswith("/api/apps/"):
            app_id = path.split("/")[-1]

            apps = load_apps()

            app = next(
                (x for x in apps if x["id"] == app_id),
                None
            )

            if app is None:
                self.send_json({
                    "success": False,
                    "error": "App not found"
                }, 404)
                return

            self.send_json({
                "success": True,
                "app": app
            })
            return

        self.send_json({
            "success": False,
            "error": "Endpoint not found"
        }, 404)

    def do_POST(self):

        path = urlparse(self.path).path

        length = int(
            self.headers.get("Content-Length", 0)
        )

        raw_body = self.rfile.read(length)

        try:
            data = json.loads(
                raw_body.decode("utf-8")
            )
        except (json.JSONDecodeError, UnicodeDecodeError):
            self.send_json({
                "success": False,
                "error": "Invalid JSON"
            }, 400)
            return

        if path == "/api/apps":

            required = [
                "name",
                "description",
                "version",
                "category"
            ]

            missing = [
                key for key in required
                if not data.get(key)
            ]

            if missing:
                self.send_json({
                    "success": False,
                    "error": "Missing fields",
                    "fields": missing
                }, 400)
                return

            apps = load_apps()

            app = {
                "id": str(uuid.uuid4()),
                "name": str(data["name"]),
                "description": str(data["description"]),
                "version": str(data["version"]),
                "category": str(data["category"]),
                "developer": str(
                    data.get("developer", "Unknown")
                ),
                "downloads": 0,
                "rating": 0.0,
                "ratingCount": 0,
                "status": "published"
            }

            apps.append(app)
            save_apps(apps)

            self.send_json({
                "success": True,
                "message": "App published",
                "app": app
            }, 201)

            return

        if path.startswith("/api/apps/") and path.endswith("/download"):

            parts = path.strip("/").split("/")

            if len(parts) != 4:
                self.send_json({
                    "success": False,
                    "error": "Invalid endpoint"
                }, 400)
                return

            app_id = parts[2]

            apps = load_apps()

            for app in apps:

                if app["id"] == app_id:

                    app["downloads"] += 1

                    save_apps(apps)

                    self.send_json({
                        "success": True,
                        "downloads": app["downloads"]
                    })
                    return

            self.send_json({
                "success": False,
                "error": "App not found"
            }, 404)

            return

        if path.startswith("/api/apps/") and path.endswith("/rating"):

            parts = path.strip("/").split("/")

            if len(parts) != 4:
                self.send_json({
                    "success": False,
                    "error": "Invalid endpoint"
                }, 400)
                return

            app_id = parts[2]

            rating = data.get("rating")

            if not isinstance(rating, (int, float)):
                self.send_json({
                    "success": False,
                    "error": "Rating must be a number"
                }, 400)
                return

            if rating < 1 or rating > 5:
                self.send_json({
                    "success": False,
                    "error": "Rating must be between 1 and 5"
                }, 400)
                return

            apps = load_apps()

            for app in apps:

                if app["id"] == app_id:

                    old_count = app["ratingCount"]
                    old_rating = app["rating"]

                    new_count = old_count + 1

                    new_rating = (
                        (old_rating * old_count) + rating
                    ) / new_count

                    app["rating"] = round(
                        new_rating,
                        2
                    )

                    app["ratingCount"] = new_count

                    save_apps(apps)

                    self.send_json({
                        "success": True,
                        "rating": app["rating"],
                        "ratingCount": new_count
                    })
                    return

            self.send_json({
                "success": False,
                "error": "App not found"
            }, 404)

            return

        self.send_json({
            "success": False,
            "error": "Endpoint not found"
        }, 404)


if __name__ == "__main__":

    host = "127.0.0.1"
    port = 8081

    server = HTTPServer(
        (host, port),
        StoreAPI
    )

    print("================================")
    print("      SHIVAM STORE API")
    print("================================")
    print(f"Server: http://{host}:{port}")
    print()
    print("GET  /api/health")
    print("GET  /api/apps")
    print("GET  /api/apps/<id>")
    print("POST /api/apps")
    print("POST /api/apps/<id>/download")
    print("POST /api/apps/<id>/rating")
    print()
    print("Press CTRL+C to stop.")
    print("================================")

    server.serve_forever()
