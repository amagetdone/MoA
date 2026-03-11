from pathlib import Path

import yaml


REPO_ROOT = Path(__file__).resolve().parents[3]
CI_CONFIG_PATH = REPO_ROOT / ".gitlab-ci.yml"


def load_ci_config() -> dict:
    return yaml.safe_load(CI_CONFIG_PATH.read_text(encoding="utf-8"))


def test_self_hosted_pipeline_does_not_use_executor_specific_images_or_services() -> None:
    config = load_ci_config()

    assert config["stages"] == ["build", "deploy"]

    for job_name in ("docker-build", "deploy"):
        job = config[job_name]
        assert "image" not in job
        assert "services" not in job


def test_self_hosted_pipeline_builds_and_deploys_locally_without_test_stage() -> None:
    config = load_ci_config()

    build_script = "\n".join(config["docker-build"]["script"])
    deploy_script = "\n".join(config["deploy"]["script"])
    deploy_before_script = "\n".join(config["deploy"].get("before_script", []))

    assert "test" not in config
    assert "docker info >/dev/null" in "\n".join(config["docker-build"]["before_script"])
    assert "docker build --pull" in build_script
    assert "docker push" not in build_script
    assert "docker run -d" in deploy_script
    assert "docker rm -f" in deploy_script
    assert "ssh " not in deploy_script
    assert "apk add" not in deploy_before_script


def test_self_hosted_pipeline_does_not_set_registry_or_dind_variables() -> None:
    config = load_ci_config()

    variables = config.get("variables", {})

    assert "DOCKER_HOST" not in variables
    assert "DOCKER_TLS_CERTDIR" not in variables
    assert "CI_REGISTRY_IMAGE" not in "\n".join(
        [
            "\n".join(config["docker-build"]["script"]),
            "\n".join(config["deploy"]["script"]),
        ]
    )
    assert "test" not in config
