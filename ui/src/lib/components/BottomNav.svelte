<script lang="ts">
	import { page } from '$app/state';
	import { HugeiconsIcon } from '@hugeicons/svelte';
	import {
		Home01Icon,
		Search01Icon,
		LibraryIcon,
		Chart02Icon
	} from '@hugeicons/core-free-icons';
	import { t } from '$lib/i18n.svelte';

	const nav = $derived([
		{ href: '/', label: t('nav.home'), icon: Home01Icon },
		{ href: '/search', label: t('nav.search'), icon: Search01Icon },
		{ href: '/library', label: t('nav.library'), icon: LibraryIcon },
		{ href: '/stats', label: t('nav.stats'), icon: Chart02Icon }
	]);

	const isActive = (href: string) =>
		href === '/' ? page.url.pathname === '/' : page.url.pathname.startsWith(href);
</script>

<!-- Spotify Mobile Bottom Navigation Bar -->
<nav
	aria-label="Mobile Navigation"
	class="fixed bottom-0 left-0 right-0 z-40 flex select-none items-center justify-around border-t border-white/5 bg-[#121212]/95 px-2 pt-2 pb-[max(env(safe-area-inset-bottom,0px),8px)] backdrop-blur-2xl transition-colors duration-200"
>
	{#each nav as item (item.href)}
		{@const active = isActive(item.href)}
		<a
			href={item.href}
			aria-current={active ? 'page' : undefined}
			class="group flex flex-1 flex-col items-center justify-center gap-1 py-1 transition-transform duration-150 active:scale-95"
		>
			<div class="relative flex items-center justify-center">
				<HugeiconsIcon
					icon={item.icon}
					strokeWidth={active ? 2.5 : 1.75}
					class="size-6 transition-colors duration-150 {active
						? 'text-white'
						: 'text-[#b3b3b3] group-hover:text-white'}"
				/>
				{#if active}
					<!-- Subtle Spotify glow beneath active icon -->
					<div
						class="absolute -inset-1 rounded-full bg-primary/20 blur-sm -z-10"
					></div>
				{/if}
			</div>
			<span
				class="text-[10.5px] tracking-tight transition-colors duration-150 {active
					? 'font-semibold text-white'
					: 'font-normal text-[#b3b3b3] group-hover:text-white'}"
			>
				{item.label}
			</span>
		</a>
	{/each}
</nav>
