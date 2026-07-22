from __future__ import annotations

from flask import current_app, session

from pybackend.db import query_one
from pybackend.user_roles import is_admin_user, sync_admin_role

_USER_SQL = "SELECT id, provider, name, email, picture, role FROM users WHERE id = %s"


def refresh_session_user() -> dict | None:
    user_id = session.get("user_id")
    if not user_id:
        session.pop("user", None)
        return None

    if not current_app.config.get("DB_AVAILABLE", True):
        return None

    settings = current_app.config["SETTINGS"]
    try:
        user = query_one(_USER_SQL, (user_id,))
    except Exception:
        return None
    user = sync_admin_role(user, settings.admin_emails)
    if user:
        session["user"] = user
        return user

    session.pop("user_id", None)
    session.pop("user", None)
    return None


def auth_template_flags() -> dict:
    user = session.get("user")
    if session.get("user_id") and not user:
        user = refresh_session_user()
    settings = current_app.config["SETTINGS"]
    return {
        "authUser": user,
        "isAdmin": is_admin_user(user, settings.admin_emails),
    }
