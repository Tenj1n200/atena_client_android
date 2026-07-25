import json

from fastapi import (
    FastAPI,
    WebSocket,
    WebSocketDisconnect,
)

from shell import execute_shell


app = FastAPI()


@app.websocket("/ws")
async def websocket_endpoint(
    websocket: WebSocket,
):
    await websocket.accept()

    try:

        while True:

            message = await websocket.receive_text()

            data = json.loads(
                message
            )

            if data.get("type") != "shell":
                continue

            command = data.get(
                "command",
                "",
            )

            cwd = data.get(
                "cwd"
            )

            result = await execute_shell(
                command=command,
                cwd=cwd,
            )

            await websocket.send_json({
                "type": "shell_result",
                "command": command,
                "cwd": cwd,
                "result": result,
            })

    except WebSocketDisconnect:

        print(
            "Cliente desconectado"
        )
