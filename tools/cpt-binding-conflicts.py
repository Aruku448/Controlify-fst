#!/usr/bin/env python3
"""
检查「持枪时 CPT 按键会不会和撞用同一手柄输入的 Controlify 默认绑定同时触发」。

背景
----
CPT 的按键（换弹 / 切换弹药 / 射击模式）与控制器的默认键位有重合：
  reload      -> button/west      与 controlify:swap_hands 撞车
  cycle_ammo  -> button/dpad_up   与 controlify:open_chat 撞车
  fire_mode   -> button/dpad_right 与 controlify:radial_menu 撞车

Controlify 依靠 InputBindingImpl.isOverriddenByManualBinding 在持枪时压制这些
默认绑定。压制条件写在 takesPriorityOverDefaults() 里：
  1. 对方是玩家手动改绑过的；或
  2. 对方是 mod 内置预设的 CPT 绑定（它等于自己的默认值，光靠条件 1 会漏掉）

本脚本用真实数据（实例 controlify.json + mod 内置 default.json）把该判定重算一遍，
任何一组没被压制的冲突都会报红。

注意：本脚本是上面那条 Java 规则的等价复现。若修改了
src/main/java/dev/isxander/controlify/bindings/InputBindingImpl.java 的判定逻辑，
请同步更新这里的 takes_priority_over_defaults()。

用法
----
    python3 tools/cpt-binding-conflicts.py [实例的 controlify.json 路径]

默认读取 ~/.minecraft/versions/RiaFst/config/controlify.json。
退出码 0 = 无冲突，1 = 有冲突。
"""
import json
import os
import sys

REPO_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DEFAULT_BINDS = os.path.join(
    REPO_ROOT, "src/main/resources/assets/controlify/controllers/default_bind/default.json"
)
DEFAULT_CONFIG = os.path.expanduser("~/.minecraft/versions/RiaFst/config/controlify.json")

CPT_KEY_PATH_PREFIX = "key.createpneumatictacticals."


def to_input(value):
    """把配置里的输入表示规范化成可比较的字符串。"""
    if value is None:
        return "EMPTY"
    if "button" in value:
        return "btn:" + value["button"]
    if "axis" in value:
        return "axis:" + value["axis"]
    return "EMPTY"


def is_cpt_binding(bind_id):
    """等价于 CptKeyBindings.isCptBinding()。"""
    return bind_id.split(":", 1)[-1].startswith(CPT_KEY_PATH_PREFIX)


def takes_priority_over_defaults(binding):
    """等价于 InputBindingImpl.takesPriorityOverDefaults()。"""
    if binding["bound"] != binding["default"]:
        return True
    return is_cpt_binding(binding["id"])


def load_bindings(config_path):
    defaults = json.load(open(DEFAULT_BINDS, encoding="utf-8"))["defaults"]
    config = json.load(open(config_path, encoding="utf-8"))

    controllers = config.get("controllers") or {}
    if not controllers:
        raise SystemExit(f"配置里没有手柄: {config_path}")
    user_bindings = next(iter(controllers.values()))["config"]["controlify:input"]["bindings"]

    bindings = []
    for bind_id, default_value in defaults.items():
        default_input = to_input(default_value)
        user_value = user_bindings.get(bind_id)
        bindings.append({
            "id": bind_id,
            "default": default_input,
            "bound": to_input(user_value) if user_value is not None else default_input,
        })
    return bindings


def main(config_path):
    bindings = load_bindings(config_path)
    cpt_bindings = [b for b in bindings if is_cpt_binding(b["id"])]
    controlify_bindings = [b for b in bindings if not is_cpt_binding(b["id"])]

    conflicts = []
    for cpt in cpt_bindings:
        if cpt["bound"] == "EMPTY":
            continue
        for target in controlify_bindings:
            if target["bound"] != cpt["bound"]:
                continue
            if not takes_priority_over_defaults(cpt):
                conflicts.append((cpt, target))

    if not conflicts:
        print(f"OK  {len(cpt_bindings)} 个 CPT 绑定，持枪时无未压制的按键冲突")
        return 0

    print(f"冲突 {len(conflicts)} 组：按住 CPT 按键时会同时触发 Controlify 默认绑定\n")
    for cpt, target in conflicts:
        print(f"  {cpt['bound']}")
        print(f"    CPT        {cpt['id'].split(':')[-1]}")
        print(f"    同时触发   {target['id']}")
    return 1


if __name__ == "__main__":
    sys.exit(main(sys.argv[1] if len(sys.argv) > 1 else DEFAULT_CONFIG))
