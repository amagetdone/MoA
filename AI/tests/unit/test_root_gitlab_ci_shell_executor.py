from pathlib import Path

import yaml


REPO_ROOT = Path(__file__).resolve().parents[3]
CI_CONFIG_PATH = REPO_ROOT / ".gitlab-ci.yml"


def load_ci_config() -> dict:
    return yaml.safe_load(CI_CONFIG_PATH.read_text(encoding="utf-8"))


def test_shell_executor_pipeline_does_not_use_docker_executor_keys() -> None:
    config = load_ci_config()

    for job_name in ("test", "docker-build", "deploy"):
        job = config[job_name]
        assert "image" not in job
        assert "services" not in job


def test_test_job_runs_python_inside_docker_and_deploy_avoids_apk() -> None:
    config = load_ci_config()

    test_script = "\n".join(config["test"]["script"])
    deploy_before_script = "\n".join(config["deploy"]["before_script"])

    assert "docker run --rm" in test_script
    assert "python:3.12.7-slim" in test_script
    assert "apk add" not in deploy_before_script


def test_shell_executor_pipeline_does_not_set_dind_only_variables() -> None:
    config = load_ci_config()

    variables = config.get("variables", {})

    assert "DOCKER_HOST" not in variables
    assert "DOCKER_TLS_CERTDIR" not in variables
