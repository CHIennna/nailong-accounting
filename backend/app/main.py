from fastapi import FastAPI

from app.api.v1.router import api_router


app = FastAPI(
    title="奶龙记账 API",
    version="0.1.0",
    description="Backend API for Nailong Accounting.",
)

app.include_router(api_router, prefix="/api/v1")


@app.get("/")
async def root() -> dict[str, str]:
    return {"name": "奶龙记账 API", "status": "ok"}
