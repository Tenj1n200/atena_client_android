import asyncio


async def execute_shell(
    command: str,
    cwd: str | None = None,
    timeout: int = 60,
):
    process = await asyncio.create_subprocess_shell(
        command,
        cwd=cwd,
        stdout=asyncio.subprocess.PIPE,
        stderr=asyncio.subprocess.PIPE,
    )

    try:
        stdout, stderr = await asyncio.wait_for(
            process.communicate(),
            timeout=timeout,
        )

    except asyncio.TimeoutError:
        process.kill()
        await process.wait()

        return {
            "success": False,
            "stdout": "",
            "stderr": f"Comando excedeu o limite de {timeout} segundos.",
            "exit_code": -1,
        }

    return {
        "success": process.returncode == 0,
        "stdout": stdout.decode(
            "utf-8",
            errors="replace",
        ),
        "stderr": stderr.decode(
            "utf-8",
            errors="replace",
        ),
        "exit_code": process.returncode,
    }
