import tomllib
from pathlib import Path


def test_pyproject_pins_torch_to_cpu_uv_index() -> None:
    with Path("pyproject.toml").open("rb") as file:
        data = tomllib.load(file)

    assert data["tool"]["uv"]["sources"]["torch"] == [{"index": "pytorch-cpu"}]
    assert data["tool"]["uv"]["index"] == [
        {
            "name": "pytorch-cpu",
            "url": "https://download.pytorch.org/whl/cpu",
            "explicit": True,
        }
    ]
