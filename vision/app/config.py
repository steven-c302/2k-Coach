from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    """Env-driven config. Matches the plan's env var names so docker-compose,
    local dev, and CI can all set the same names."""

    model_config = SettingsConfigDict(env_prefix="NBA2K_VISION_")

    core_base_url: str = "http://localhost:8080"
    capture_interval_seconds: float = 1.5
    capture_backend: str = "mss"  # swap to "dxcam"/"bettercam" later without touching downstream code


settings = Settings()
