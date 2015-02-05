package com.mgmtp.perfload.perfalyzer.reporting;

import com.googlecode.jatl.HtmlWriter;

import java.util.Map;
import java.util.Set;

/**
 * @author rnaegele
 */
public class NavBar extends HtmlWriter {

	private final Set<String> tabNames;
	private final Map<String, QuickJump> quickJumps;

	public NavBar(final Set<String> tabNames, final Map<String, QuickJump> quickJumps) {
		this.tabNames = tabNames;
		this.quickJumps = quickJumps;
	}

	@Override
	protected void build() {
		start("nav").id("navigation");
			div().classAttr("perf-navbar-header");
				a().href("").text("perfAlyzer Report").end();
			end();
			div().classAttr("perf-navbar-content");
				// only if markers are present:
				if (tabNames.size() > 1) {
					ul().attr("role", "tablist");
					boolean active = true;
					for (String tab : tabNames) {
						li().attr("role", "presentation");
							if (active) {
								classAttr("active");
								active = false;
							}
							a().href("#" + tab).id("tab_" + tab).attr("data-toggle", "tab", "role", "tab").text(tab).end();
						end();
					}
					end();
				}

				ul().classAttr("perf-quickjump");
					boolean active = true;
					for (String tab : tabNames) {
						li().id("quickjump_" + tab);
							if (active) {
								classAttr("dropdown show");
								active = false;
							} else {
								classAttr("dropdown hide");
							}
							a().classAttr("dropdown-toggle").href("#").attr("data-toggle", "dropdown", "aria-expanded", "false");
								text("Quick Jump");
								span().classAttr("caret").end();
							end();
							ul().classAttr("dropdown-menu").attr("role", "menu");
								QuickJump quickJump = quickJumps.get(tab);
								quickJump.getEntryMap().entrySet().forEach(entry -> {
									li();
										a();
											href("#" + entry.getKey()).text(entry.getValue());
										end();
									end();
								});
							end();
						end();
					}
				end();
			end();
		end();
	}
}
