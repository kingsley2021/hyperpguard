package io.github.vivibibi30.hyperpguard;

import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 三个页面构建方法共享的界面状态。
 *
 * 负责：分组开关（按包名）、作用域提示文本、缺组件分组、每页的滚动容器。
 * 不负责：构建界面、读写配置、连服务——那些在 MainActivity 与各工具类里。
 *
 * 这些字段被 buildGeneralPage / buildComponentsPage / buildSettingsPage 同时读写，
 * 所以放在一个对象里：页面方法哪天要搬到独立类，传这个对象就行，不用给每个字段挂 host。
 */
final class PageState {

    /** 每个分组的开关视图，key = 包名 */
    final Map<String, Switch> groupSwitches = new LinkedHashMap<>();

    /** 每个分组右侧的作用域提示文本，key = 包名 */
    final Map<String, TextView> scopeHints = new LinkedHashMap<>();

    /** 本机没有安装的分组包名（卡片上要标注「未安装」） */
    final Set<String> missingGroups = new HashSet<>();

    /** 三页的滚动容器，切页与恢复滚动位置都要用 */
    final List<ScrollView> pageScrolls = new ArrayList<>();
}
