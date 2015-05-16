#
# spec file for package esmeska (Version 2008c)
#
# Copyright (c) 2008 SUSE LINUX Products GmbH, Nuernberg, Germany.
#
# All modifications and additions to the file contributed by third parties
# remain the property of their copyright owners, unless otherwise agreed
# upon. The license for this file, and modifications and additions to the
# file, is the same license as for the pristine package itself (unless the
# license for the pristine package is not an Open Source License, in which
# case the license is the MIT License). An "Open Source License" is a
# license that conforms to the Open Source Definition (Version 1.9)
# published by the Open Source Initiative.

# Please submit bugfixes or comments via http://bugs.opensuse.org/
#

# norootforbuild

Name:           esmska
Version:        1.9
Release:        0
Summary:        Sending SMS over the Internet
Group:          System/X11/Utilities
License:        AGPLv3
URL:            https://github.com/kparal/esmska
Source:         %{name}-%{version}.tar.bz2
BuildRoot:      %{_tmppath}/%{name}-%{version}-build
BuildArch:      noarch
Requires:       jre >= 1.6.0
Requires:       rhino
BuildRequires:  java-devel >= 1.6.0
BuildRequires:  ant >= 1.8.0
BuildRequires:  ant-junit
BuildRequires:	desktop-file-utils
# fontconfig requires some font, which doesn't seem to be chosen
# automatically by OBS. Fonts are named differently in Fedora and OpenSUSE.
%if 0%{?fedora}
BuildRequires:	dejavu-sans-fonts
%else
BuildRequires:  dejavu-fonts
%endif


%description
Program for sending SMS over the Internet.
 * Send SMS to various gateways (local or international) 
 * Supports all common operating systems (Linux, Windows, Mac OS, etc.) 
 * Free, under free/open-source licence GNU AGPL 
 * Import contacts from other programs (DreamCom) and formats (vCard) 
 * Send SMS to multiple recipients at once 
 * History of sent messages 
 * Pluggable gateway system - easy to provide support for more gateways directly by users 
 * Extensive possibilities of changing appearance 
 * Many other planned features

%package javadoc
Summary:        Javadoc for %{name}
Group:          Development/Libraries/Java
Requires(post): /bin/ln
Requires(post): /bin/rm
Requires(postun): /bin/rm

%description javadoc
This package contains a javadoc for %{name}.

%prep
%setup -q

# update config file
# don't announce program updates
sed -i -r 's/^#?\s*announceProgramUpdates.*/announceProgramUpdates = no/' include/%{name}.conf

%build
# some files names needs UTF-8
LC_ALL="en_US.UTF-8" ant

%install
cd dist
install -d -m 755 $RPM_BUILD_ROOT/%{_datadir}/%{name}
install -m 0644 %{name}.jar $RPM_BUILD_ROOT/%{_datadir}/%{name}/
install -m 0755 %{name}.sh $RPM_BUILD_ROOT/%{_datadir}/%{name}/
# jars
install -d -m 0755 $RPM_BUILD_ROOT/%{_datadir}/%{name}/lib
install -m 0644 lib/* $RPM_BUILD_ROOT/%{_datadir}/%{name}/lib
# other files
cp -r gateways $RPM_BUILD_ROOT/%{_datadir}/%{name}
# javadoc
install -d -m 755 $RPM_BUILD_ROOT/%{_javadocdir}/%{name}
cp -pr javadoc/* $RPM_BUILD_ROOT/%{_javadocdir}/%{name}
# esmska bin
install -d -m 0755 $RPM_BUILD_ROOT/%{_bindir}/
ln -sf ../../%{_datadir}/%{name}/%{name}.sh $RPM_BUILD_ROOT/%{_bindir}/%{name}
# icon
install -d -m 755 $RPM_BUILD_ROOT%{_datadir}/pixmaps
install -m 644 icons/%{name}.svg $RPM_BUILD_ROOT%{_datadir}/pixmaps
# esmska.conf
install -d -m 0755 $RPM_BUILD_ROOT%{_sysconfdir}/%{name}
install -m 0644 %{name}.conf $RPM_BUILD_ROOT%{_sysconfdir}/%{name}
ln -sf %{_sysconfdir}/%{name}/%{name}.conf $RPM_BUILD_ROOT/%{_datadir}/%{name}/%{name}.conf
# desktop menu
install -d -m 755 $RPM_BUILD_ROOT%{_datadir}/applications
desktop-file-install                 \
        --add-category Application                       \
        --dir $RPM_BUILD_ROOT%{_datadir}/applications    \
        --vendor esmska \
        ../resources/%{name}.desktop

%clean
rm -rf $RPM_BUILD_ROOT

%files
%defattr(0755,root,root,0755)
%{_bindir}/%{name}
%defattr(-,root,root,0755)
%doc dist/license/license.txt dist/license/gnu-agpl.txt dist/readme.txt
%dir %{_sysconfdir}/%{name}/
%config(noreplace) %{_sysconfdir}/%{name}/%{name}.conf
%{_datadir}/%{name}
%{_datadir}/pixmaps/%{name}.svg
%{_datadir}/applications/%{name}.desktop

%files javadoc
%defattr(-,root,root)
%{_javadocdir}/%{name}


%changelog
* Tue Dec 16 2008 - mvyskocil@suse.cz
- Initial packaging of esmeska 0.12.1

