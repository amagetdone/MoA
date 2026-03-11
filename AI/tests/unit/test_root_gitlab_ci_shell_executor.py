from pathlib import Path

import yaml


REPO_ROOT = Path(__file__).resolve().parents[3]
CI_CONFIG_PATH = REPO_ROOT / ".gitlab-ci.yml"


def load_ci_config() -> dict:
    return yaml.safe_load(CI_CONFIG_PATH.read_text(encoding="utf-8"))


def test_docker_executor_pipeline_uses_job_images_and_services() -> None:
    config = load_ci_config()

    assert config["test"]["image"] == "python:3.12.7-slim"
    assert config["docker-build"]["image"] == "docker:27-cli"
    assert config["docker-build"]["services"] == [{"name": "docker:27-dind"}]
    assert config["deploy"]["image"] == "alpine:3.21"


def test_test_job_runs_directly_in_python_image_and_deploy_installs_ssh_client() -> None:
    config = load_ci_config()

    test_script = "\n".join(config["test"]["script"])
    deploy_before_script = "\n".join(config["deploy"]["before_script"])
    test_before_script = "\n".join(config["test"]["before_script"])

    assert "docker run --rm" not in test_script
    assert "python -m pip install --no-cache-dir uv" in test_before_script
    assert "cd AI" in test_script
    assert "uv sync --frozen --extra dev" in test_script
    assert ".venv/bin/python -m pytest -q" in test_script
    assert "apk add --no-cache openssh-client" in deploy_before_script


def test_docker_executor_pipeline_sets_dind_variables() -> None:
    config = load_ci_config()

    variables = config.get("variables", {})

    assert variables["DOCKER_HOST"] == "tcp://docker:2375"
    assert variables["DOCKER_TLS_CERTDIR"] == ""
