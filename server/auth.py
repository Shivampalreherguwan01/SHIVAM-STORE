import hashlib
import secrets
import sqlite3
from datetime import datetime, timedelta, timezone

DB_FILE = "server/data/store.db"


def get_db():
    conn = sqlite3.connect(DB_FILE)
    conn.row_factory = sqlite3.Row
    return conn


def hash_password(password):
    salt = secrets.token_bytes(16)

    password_hash = hashlib.pbkdf2_hmac(
        "sha256",
        password.encode("utf-8"),
        salt,
        200_000
    )

    return salt.hex() + ":" + password_hash.hex()


def verify_password(password, stored_hash):
    try:
        salt_hex, hash_hex = stored_hash.split(":")

        salt = bytes.fromhex(salt_hex)

        calculated = hashlib.pbkdf2_hmac(
            "sha256",
            password.encode("utf-8"),
            salt,
            200_000
        )

        return secrets.compare_digest(
            calculated.hex(),
            hash_hex
        )

    except (ValueError, TypeError):
        return False


def create_session(user_id):
    token = secrets.token_urlsafe(32)

    expires_at = (
        datetime.now(timezone.utc)
        + timedelta(days=30)
    ).isoformat()

    conn = get_db()

    conn.execute(
        """
        INSERT INTO sessions
        (token, user_id, expires_at)
        VALUES (?, ?, ?)
        """,
        (token, user_id, expires_at)
    )

    conn.commit()
    conn.close()

    return token


def get_user_from_token(token):
    if not token:
        return None

    conn = get_db()

    row = conn.execute(
        """
        SELECT
            users.id,
            users.name,
            users.email,
            users.developer_enabled,
            users.created_at,
            sessions.expires_at
        FROM sessions
        JOIN users
            ON users.id = sessions.user_id
        WHERE sessions.token = ?
        """,
        (token,)
    ).fetchone()

    conn.close()

    if row is None:
        return None

    try:
        expires_at = datetime.fromisoformat(
            row["expires_at"]
        )

        if expires_at <= datetime.now(timezone.utc):
            return None

    except ValueError:
        return None

    return dict(row)
