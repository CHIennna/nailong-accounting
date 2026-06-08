import json
from typing import Any

import httpx

from app.core.config import settings


class DeepSeekClient:
    async def create_json_chat_completion(
        self,
        messages: list[dict[str, str]],
    ) -> dict[str, Any]:
        if not settings.deepseek_api_key:
            raise RuntimeError("DEEPSEEK_API_KEY is not configured")

        payload: dict[str, Any] = {
            "model": settings.deepseek_model,
            "messages": messages,
            "temperature": 0.4,
            "response_format": {"type": "json_object"},
        }

        async with httpx.AsyncClient(timeout=settings.ai_request_timeout_seconds) as client:
            response = await client.post(
                f"{settings.deepseek_base_url.rstrip('/')}/chat/completions",
                headers={
                    "Authorization": f"Bearer {settings.deepseek_api_key}",
                    "Content-Type": "application/json",
                },
                json=payload,
            )
            response.raise_for_status()

        data = response.json()
        content = data["choices"][0]["message"]["content"]
        return json.loads(content)
