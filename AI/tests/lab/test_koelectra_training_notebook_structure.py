import json
from pathlib import Path


def test_training_notebook_has_no_kfold_flow() -> None:
    notebook_path = Path("lab/koelectra_merchant_training_kfold.ipynb")
    notebook = json.loads(notebook_path.read_text(encoding="utf-8"))
    all_source = "\n".join("".join(cell.get("source", [])) for cell in notebook["cells"])

    assert "GroupKFold" not in all_source
    assert "cv_df =" not in all_source
    assert "kfold/cv_metrics.csv" not in all_source
    assert "load_hf_cv_metrics" not in all_source
    assert "## GroupKFold Results" not in all_source
    assert "NUM_FOLDS" not in all_source
    assert "Final Training" in all_source


def test_training_notebook_has_no_early_stopping() -> None:
    notebook_path = Path("lab/koelectra_merchant_training_kfold.ipynb")
    notebook = json.loads(notebook_path.read_text(encoding="utf-8"))
    all_source = "\n".join("".join(cell.get("source", [])) for cell in notebook["cells"])

    assert "EarlyStoppingCallback" not in all_source
    assert "EARLY_STOPPING_PATIENCE" not in all_source
    assert "early_stopping_patience" not in all_source


def test_training_notebook_enables_beauty_eval_augmentation() -> None:
    notebook_path = Path("lab/koelectra_merchant_training_kfold.ipynb")
    notebook = json.loads(notebook_path.read_text(encoding="utf-8"))
    all_source = "\n".join("".join(cell.get("source", [])) for cell in notebook["cells"])

    assert "MERCHANT_INFO_EVAL_AUGMENTED_CATEGORIES" in all_source
    assert "MERCHANT_INFO_EVAL_SPLIT_RATIOS" in all_source
    assert "eval_augmented_categories=MERCHANT_INFO_EVAL_AUGMENTED_CATEGORIES" in all_source
    assert "eval_split_ratio=MERCHANT_INFO_EVAL_SPLIT_RATIOS" in all_source


def test_training_notebook_reloads_merchant_info_augmentation_module() -> None:
    notebook_path = Path("lab/koelectra_merchant_training_kfold.ipynb")
    notebook = json.loads(notebook_path.read_text(encoding="utf-8"))
    all_source = "\n".join("".join(cell.get("source", [])) for cell in notebook["cells"])

    assert "import importlib" in all_source
    assert "import merchant_info_augmentation" in all_source
    assert "merchant_info_augmentation = importlib.reload(merchant_info_augmentation)" in all_source
    assert "augment_dataset_splits = merchant_info_augmentation.augment_dataset_splits" in all_source


def test_training_notebook_preserves_korean_text_sections() -> None:
    notebook_path = Path("lab/koelectra_merchant_training_kfold.ipynb")
    notebook = json.loads(notebook_path.read_text(encoding="utf-8"))
    all_source = "\n".join("".join(cell.get("source", [])) for cell in notebook["cells"])

    assert "이 노트북은 업로드된 merchant category dataset을 사용해 다음을 수행합니다." in all_source
    assert "기본 전제:" in all_source
    assert "# 입력 텍스트 구성" in all_source
    assert "최종 모델은 다음 방식으로 학습합니다." in all_source
    assert "스타벅스 강남R점 [SEP] 스타벅스 강남R점" in all_source
