from fastapi import APIRouter

from app.api.v1.ai import router as ai_router


api_router = APIRouter()


@api_router.get("/health")
async def health() -> dict[str, str]:
    return {"status": "ok"}


api_router.include_router(ai_router, prefix="/ai", tags=["AI"])
