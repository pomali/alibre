# Alibre - Android Ebook Reader - LLM Agent Documentation

This documentation is designed to help LLM agents understand and work with the Alibre Android ebook reader application.

## Project Overview

**Alibre** is an Android application for reading and displaying ebooks with support for multiple formats including PDF, TXT, EPUB, HTML, MOBI, and AZW.

## Core Features

### Library Management
- Add folders containing ebooks
- Automatic recursive scanning for supported formats
- Persistent storage of book metadata
- Clean, card-based UI for book display

### Reading Experience
- Configurable reading interface
- Reading position memory
- Last opened timestamp tracking

### Supported Formats
- PDF documents
- Plain text files (TXT)
- EPUB ebooks
- HTML documents
- MOBI ebooks
- AZW/AZW3 Kindle formats
- FB2 and RTF files

## Key Components

### Main Screens
1. **LibraryFragment** - Browse and manage ebook collection
2. **ReadingFragment** - Display ebook content

### Core Classes
- `FileScanner` - Handles folder scanning and book discovery
- `BooksAdapter` - RecyclerView adapter for book display
- `LibraryFoldersAdapter` - RecyclerView adapter for library folder display
- `LibraryViewModel` - MVVM data management
- `AppDatabase` - Room database configuration

### Data Models
- `Book` - Ebook metadata and reading state
- `LibraryFolder` - Tracked library folder locations

## Recent Changes

The application was recently enhanced to fix library functionality:
- Implemented automatic folder scanning after folder selection
- Added RecyclerView adapter for book display
- Created proper MVVM architecture with ViewModel
- Enhanced UI with loading states and empty state handling
- Added comprehensive file format support
- **NEW**: Added folder management interface to view and remove library folders
- **NEW**: Enhanced library UI with separate sections for folders and books
- **NEW**: Added confirmation dialogs for folder removal with clear explanations
- **NEW**: Automatic cleanup of books when folders are removed

## Getting Started for LLM Agents

1. **Project Setup**: Java 17 required for building
2. **Build Command**: `./gradlew assembleDebug` (with JAVA_HOME set to Java 17)
3. **Key Files to Understand**: Start with `LibraryFragment.kt` and `FileScanner.kt`
4. **Database**: Room database with automatic migrations enabled
5. **Navigation**: Uses Android Navigation Component

## Architecture Patterns Used

- **MVVM**: Model-View-ViewModel pattern for UI components
- **Repository Pattern**: Data access through DAOs
- **Observer Pattern**: LiveData for reactive UI updates
- **Storage Access Framework**: For modern Android file access

Refer to the detailed guides in this documentation folder for specific implementation details and development workflows.
